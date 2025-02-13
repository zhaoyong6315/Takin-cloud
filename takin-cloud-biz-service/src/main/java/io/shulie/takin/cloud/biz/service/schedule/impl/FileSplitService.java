package io.shulie.takin.cloud.biz.service.schedule.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;

import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.pamirs.takin.entity.domain.entity.scene.manage.SceneFileReadPosition;
import com.pamirs.takin.entity.domain.vo.file.FileSliceRequest;
import io.shulie.takin.cloud.biz.service.engine.EngineConfigService;
import io.shulie.takin.cloud.biz.service.scene.SceneManageService;
import io.shulie.takin.cloud.biz.service.schedule.FileSliceService;
import io.shulie.takin.cloud.biz.service.schedule.ScheduleService;
import io.shulie.takin.cloud.common.bean.scenemanage.UpdateStatusBean;
import io.shulie.takin.cloud.common.constants.*;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneRunTaskStatusEnum;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.cloud.common.utils.FileSliceByLine.FileSliceInfo;
import io.shulie.takin.cloud.common.utils.FileSliceByPodNum;
import io.shulie.takin.cloud.common.utils.FileSliceByPodNum.Builder;
import io.shulie.takin.cloud.common.utils.FileSliceByPodNum.StartEndPair;
import io.shulie.takin.cloud.data.model.mysql.SceneBigFileSliceEntity;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.eventcenter.annotation.IntrestFor;
import io.shulie.takin.ext.content.enginecall.ScheduleRunRequest;
import io.shulie.takin.ext.content.enginecall.ScheduleStartRequestExt;
import io.shulie.takin.ext.content.enginecall.ScheduleStartRequestExt.DataFile;
import io.shulie.takin.ext.content.enginecall.ScheduleStartRequestExt.StartEndPosition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author 莫问
 * @date 2020-08-07
 */

@Service
@Slf4j
public class FileSplitService {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private FileSliceService fileSliceService;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private SceneManageService sceneManageService;

    @Autowired
    private EngineConfigService engineConfigService;

    @Value("${script.path}")
    private String nfsDir;

    private static final String DEFAULT_PATH_SEPARATOR = "/";

    private static final String CSV_SUFFIX = "csv";

    private static final String TXT_SUFFIX = "txt";

    @IntrestFor(event = ScheduleEventConstant.INIT_SCHEDULE_EVENT)
    public void initSchedule(Event event) {
        fileSplit((ScheduleRunRequest)event.getExt());
    }

    /**
     * 分件分割
     */
    public void fileSplit(ScheduleRunRequest request) {
        ScheduleStartRequestExt startRequest = request.getRequest();
        try {
            List<DataFile> dataFiles = generateFileSlice(startRequest);
            request.getRequest().setDataFile(dataFiles);
        }catch (Exception e){
            //更新场景状态：启动中--待启动
            sceneManageService.updateSceneLifeCycle(
                    UpdateStatusBean.build(startRequest.getSceneId(),
                                    startRequest.getTaskId(),
                                    startRequest.getCustomerId()).checkEnum(
                                    SceneManageStatusEnum.STARTING)
                            .updateEnum(SceneManageStatusEnum.WAIT)
                            .build());
            log.error(e.getMessage());

            //设置运行状态为启动失败
            String key = String.format(SceneTaskRedisConstants.SCENE_TASK_RUN_KEY + "%s_%s", startRequest.getSceneId(),
                    startRequest.getTaskId());
            redisTemplate.opsForHash().put(key, SceneTaskRedisConstants.SCENE_RUN_TASK_STATUS_KEY,
                    SceneRunTaskStatusEnum.FAILED.getText());
            redisTemplate.opsForHash().put(key, SceneTaskRedisConstants.SCENE_RUN_TASK_ERROR,
                    String.format("启动场景失败:场景ID:%s,文件拆分异常",startRequest.getSceneId()));

            throw new TakinCloudException(TakinCloudExceptionEnum.SCENE_CSV_FILE_SPLIT_ERROR,
                    "启动场景失败:场景ID:" + request.getRequest().getSceneId() + ",文件拆分异常");
        }
        scheduleService.runSchedule(request);
    }

    private List<DataFile> generateFileSlice(ScheduleStartRequestExt startRequest) {
        if (CollectionUtils.isEmpty(startRequest.getDataFile()) || startRequest.getDataFile().size() == 0) {
            return null;
        }
        Map<String, List<DataFile>> dataFileMap = analyzeDatafiles(startRequest.getDataFile());
        if (dataFileMap == null || dataFileMap.isEmpty()) {
            return null;
        }
        List<DataFile> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(dataFileMap.get(FileSplitConstants.NO_NEED_SPLIT_KEY))) {
            result.addAll(dataFileMap.get(FileSplitConstants.NO_NEED_SPLIT_KEY));
        }
        List<DataFile> needToSplit = dataFileMap.get(FileSplitConstants.NEED_SPLIT_KEY);
        if (CollectionUtils.isEmpty(needToSplit)) {
            return result;
        }
        AtomicBoolean sliceResult = new AtomicBoolean(true);
        result.addAll(needToSplit.stream().peek(dataFile -> {
            int totalPod = startRequest.getTotalIp();
            FileSliceRequest fileSliceRequest = new FileSliceRequest() {{
                setSceneId(startRequest.getSceneId());
                setRefId(dataFile.getRefId());
                setFilePath(dataFile.getPath());
                setFileName(dataFile.getName());
                setPodNum(totalPod);
                setSplit(dataFile.isSplit());
                setOrderSplit(dataFile.isOrdered());
                setBigFile(dataFile.isBigFile());
                setForceSplit(false);
            }};
            //根据场景判断是否是预分片的数据，预分片的数据一定是按照顺序分片的,预分片的文件是挂载到磁盘上的，不在nfs
            boolean fileSliced = false;
            String[] localMountSceneIds = engineConfigService.getLocalMountSceneIds();
            if (localMountSceneIds != null && localMountSceneIds.length > 0
                    && ArrayUtils.contains(localMountSceneIds, startRequest.getSceneId() + "")) {
                fileSliced = true;
                dataFile.setOrdered(true);
            }
            if (fileSliced || fileSliceService.fileSlice(fileSliceRequest)) {
                List<StartEndPair> pairs = new ArrayList<>();
                SceneBigFileSliceEntity sliceEntity = fileSliceService.getOneByParam(fileSliceRequest);
                if (Objects.isNull(sliceEntity)) {
                    log.error("启动场景失败:场景ID:{},文件名:{}，未查询到分片信息", startRequest.getSceneId(),dataFile.getName());
                    sliceResult.set(false);
                    return;
                }
                //文件由拆分改为不拆分
                if (!dataFile.isSplit() && sliceEntity.getSliceCount() > 1) {
                    fileSliceRequest.setPodNum(1);
                    fileSliceRequest.setForceSplit(true);
                    fileSliced = fileSliceService.fileSlice(fileSliceRequest);
                    if (fileSliced) {
                        sliceEntity = fileSliceService.getOneByParam(fileSliceRequest);
                    }
                }
                //非大文件上传的或者不需要按顺序分片的，如果pod与分片数量不一致，重新分片
                else if (!dataFile.isBigFile() || !dataFile.isOrdered()) {
                    if (totalPod != sliceEntity.getSliceCount()) {
                        fileSliceRequest.setForceSplit(true);
                        fileSliced = fileSliceService.fileSlice(fileSliceRequest);
                        if (fileSliced) {
                            sliceEntity = fileSliceService.getOneByParam(fileSliceRequest);
                        }
                    }
                }
                //大文件按顺序分片，如果pod数量与分片数量不一致，抛出异常
                else if (totalPod != sliceEntity.getSliceCount()) {
                    log.error("启动场景失败：场景ID:{},文件名:{},异常信息：pod数量:{}与文件分片数量{}不匹配", startRequest.getSceneId(),
                            dataFile.getName(),totalPod,sliceEntity.getSliceCount());
                    sliceResult.set(false);
                    return;
                }
                List<FileSliceInfo> list = JSONObject.parseArray(sliceEntity.getSliceInfo(), FileSliceInfo.class);
                for (int i = 0, size = list.size(); i < size; i++) {
                    StartEndPair pair = new StartEndPair();
                    pair.setEnd(list.get(i).getEnd());
                    pair.setPartition(list.get(i).getPartition() + "");
                    //如果文件要继续从上次读取的位置继续读，从缓存中取数据，替换开始位置
                    if (startRequest.getFileContinueRead()) {
                        String key = String.format(SceneStartCheckConstants.SCENE_KEY, startRequest.getSceneId());
                        Map<Object, Object> positionMap = redisTemplate.opsForHash().entries(key);
                        String podKey = String.format(SceneStartCheckConstants.FILE_POD_FIELD_KEY, dataFile.getName(), i + 1);
                        Object podReadPosition = positionMap.get(podKey);
                        if (Objects.nonNull(podReadPosition)) {
                            SceneFileReadPosition position = JSONUtil.toBean(podReadPosition.toString(),
                                    SceneFileReadPosition.class);
                            if (position.getReadPosition() >= list.get(i).getStart()
                                    && position.getReadPosition() <= list.get(i).getEnd()) {
                                pair.setStart(position.getReadPosition());
                            } else {
                                pair.setStart(list.get(i).getStart());
                            }
                        } else {
                            pair.setStart(list.get(i).getStart());
                        }
                    } else {
                        pair.setStart(list.get(i).getStart());
                    }
                    pairs.add(pair);
                }
                fillDataFile(dataFile, pairs, startRequest.getTotalIp());
            } else {
                log.error("启动场景失败:场景ID:{},文件分片出错", startRequest.getSceneId());
                sliceResult.set(false);
            }
        }).collect(Collectors.toList()));
        if (!sliceResult.get()) {
            throw new TakinCloudException(TakinCloudExceptionEnum.SCENE_CSV_FILE_SPLIT_ERROR,
                    "启动压测场景--场景ID:" + startRequest.getSceneId() + ",文件分片失败");
        }
        return result;
    }

    /**
     * 多个pod共同读取同一个文件,每个pod读取不同的位置,即每个pod读取不同的partition(如果按照partition排序)
     */
    private void fillDataFile(DataFile dataFile, List<StartEndPair> startEndPairs, int porNum) {
        Map<Integer, List<StartEndPosition>> startEndPositions = dataFile.getStartEndPositions();
        if (startEndPositions == null) {
            startEndPositions = new HashMap<>(porNum);
        }
        if (!dataFile.isSplit() && porNum > 1 && startEndPairs.size() == 1) {
            for (int i = 0; i < porNum; i++) {
                startEndPositions.put(i, Lists.newArrayList(new StartEndPosition() {{
                    setStart(String.valueOf(startEndPairs.get(0).getStart()));
                    setEnd(String.valueOf(startEndPairs.get(0).getEnd()));
                    if (StringUtils.isNotBlank(startEndPairs.get(0).getPartition())) {
                        setPartition(startEndPairs.get(0).getPartition());
                    }
                }}));
            }
            dataFile.setStartEndPositions(startEndPositions);
            return;
        }
        int remainder = startEndPairs.size() % porNum;
        int everyPodNumber = startEndPairs.size() / porNum;
        int offset = 0;
        for (int i = 0; i < porNum; i++) {
            List<StartEndPair> values;
            if (remainder > 0) {
                values = startEndPairs.subList(i * everyPodNumber + offset, (i + 1) * everyPodNumber + offset + 1);
                remainder--;
                offset++;
            } else {
                values = startEndPairs.subList(i * everyPodNumber + offset, (i + 1) * everyPodNumber + offset);
            }
            List<StartEndPosition> positions = startEndPositions.get(i);
            if (positions == null) {
                positions = new ArrayList<>();
            }
            positions.addAll(values.stream().map(
                value -> new StartEndPosition() {{
                    setStart(String.valueOf(value.getStart()));
                    setEnd(String.valueOf(value.getEnd()));
                    setPartition(String.valueOf(value.getPartition()));
                }}
            ).collect(Collectors.toList()));
            if (positions.size() > 0) {
                startEndPositions.put(i, positions);
            }
        }
        dataFile.setStartEndPositions(startEndPositions);
    }

    private List<StartEndPair> slice(String filePath) {
        try {
            String absPath = nfsDir + DEFAULT_PATH_SEPARATOR + filePath;
            FileSliceByPodNum build = new Builder(absPath)
                .withPartSize(1)
                .build();
            ArrayList<StartEndPair> startEndPairs = build.getStartEndPairs();
            if (startEndPairs == null || startEndPairs.size() == 0) {
                File file = new File(absPath);
                if (!file.exists() && !file.isFile()) {
                    throw new IOException("文件不存在或不是文件" + absPath);
                } else {
                    return Collections.singletonList(new StartEndPair() {{
                        setStart(0);
                        setEnd(file.length() - 1);
                    }});
                }
            }
            return startEndPairs;
        } catch (Exception e) {
            log.error("计算文件大小：出错--{}", e.toString());
        }
        return null;
    }

    private Map<String,List<DataFile>> analyzeDatafiles(List<DataFile> dataFiles){
        Map<String,List<DataFile>> resultMap = new HashMap<>(2);
        List<DataFile> needSplit = new ArrayList<>();
        List<DataFile> noNeedSplit = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(dataFiles)){
            Map<Integer, List<DataFile>> fileTypeMap = dataFiles.stream().collect(
                Collectors.groupingBy(DataFile::getFileType));
            List<DataFile> noNeed = fileTypeMap.get(FileSplitConstants.FILE_TYPE_EXTRA_FILE);
            if (CollectionUtils.isNotEmpty(noNeed)){
                noNeedSplit.addAll(noNeed);
            }
            List<DataFile> need = fileTypeMap.get(FileSplitConstants.FILE_TYPE_DATA_FILE);
            if (CollectionUtils.isNotEmpty(need)) {
                Map<String, List<DataFile>> collect = need.stream().collect(Collectors.groupingBy(
                    dataFile -> dataFile.getName().substring(dataFile.getName().lastIndexOf(".") + 1).toLowerCase()));
                if (collect != null && !collect.isEmpty()) {
                    for (Map.Entry<String, List<DataFile>> entry : collect.entrySet()) {
                        if (CSV_SUFFIX.equals(entry.getKey()) || TXT_SUFFIX.equals(entry.getKey())) {
                            needSplit.addAll(entry.getValue());
                        } else {
                            noNeedSplit.addAll(entry.getValue());
                        }
                    }
                }
            }
            resultMap.put(FileSplitConstants.NEED_SPLIT_KEY,needSplit);
            resultMap.put(FileSplitConstants.NO_NEED_SPLIT_KEY,noNeedSplit);
            return resultMap;
        }
        return null;
    }
}
