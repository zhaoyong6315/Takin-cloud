package io.shulie.takin.cloud.open.entrypoint.controller.scenemanage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;

import com.alibaba.fastjson.JSON;

import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import io.shulie.takin.cloud.biz.cache.DictionaryCache;
import io.shulie.takin.cloud.biz.input.scenemanage.SceneManageQueryInput;
import io.shulie.takin.cloud.biz.input.scenemanage.SceneManageWrapperInput;
import io.shulie.takin.cloud.biz.input.scenemanage.SceneSlaRefInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageListOutput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.service.scene.SceneManageService;
import io.shulie.takin.cloud.biz.service.strategy.StrategyConfigService;
import io.shulie.takin.cloud.biz.utils.SlaUtil;
import io.shulie.takin.cloud.common.bean.collector.SendMetricsEvent;
import io.shulie.takin.cloud.common.bean.scenemanage.SceneManageQueryOpitons;
import io.shulie.takin.cloud.common.constants.APIUrls;
import io.shulie.takin.cloud.common.constants.DicKeyConstant;
import io.shulie.takin.cloud.common.constants.SceneManageConstant;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.cloud.common.request.scenemanage.UpdateSceneFileRequest;
import io.shulie.takin.cloud.common.utils.CloudPluginUtils;
import io.shulie.takin.cloud.common.utils.EnginePluginUtils;
import io.shulie.takin.cloud.common.utils.ListHelper;
import io.shulie.takin.cloud.open.entrypoint.convert.SceneBusinessActivityRefInputConvert;
import io.shulie.takin.cloud.open.entrypoint.convert.SceneScriptRefInputConvert;
import io.shulie.takin.cloud.open.entrypoint.convert.SceneSlaRefInputConverter;
import io.shulie.takin.cloud.open.entrypoint.convert.SceneTaskOpenConverter;
import io.shulie.takin.cloud.open.req.scenemanage.SceneManageDeleteReq;
import io.shulie.takin.cloud.open.req.scenemanage.SceneManageWrapperReq;
import io.shulie.takin.cloud.open.req.scenemanage.ScriptCheckAndUpdateReq;
import io.shulie.takin.cloud.open.resp.scenemanage.BusinessActivityDetailResp;
import io.shulie.takin.cloud.open.resp.scenemanage.SceneDetailResp;
import io.shulie.takin.cloud.open.resp.scenemanage.SceneManageListResp;
import io.shulie.takin.cloud.open.resp.scenemanage.SceneManageWrapperResp;
import io.shulie.takin.cloud.open.resp.scenemanage.ScriptCheckResp;
import io.shulie.takin.cloud.open.resp.strategy.StrategyResp;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.shulie.takin.ext.content.enginecall.StrategyOutputExt;
import io.shulie.takin.ext.content.script.ScriptVerityRespExt;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author qianshui
 * @date 2020/4/17 下午2:31
 */
@Slf4j
@RestController
@RequestMapping(APIUrls.TRO_OPEN_API_URL + "scenemanage")
@Api(tags = "压测场景管理")
public class SceneManageOpenController {

    @Autowired
    private SceneManageService sceneManageService;

    @Autowired
    private StrategyConfigService strategyConfigService;

    @Autowired
    private DictionaryCache dictionaryCache;

    @Autowired
    private EnginePluginUtils pluginUtils;

    @ApiOperation(value = "|_ 更改脚本对应的压测场景的文件")
    @PutMapping("/updateFile")
    public ResponseResult<Object> updateFile(@RequestBody @Validated UpdateSceneFileRequest request) {
        sceneManageService.updateFileByScriptId(request);
        return ResponseResult.success();
    }

    @GetMapping(value = "/query/ids")
    public ResponseResult<List<SceneManageWrapperOutput>> getByIds(@RequestParam("sceneIds") List<Long> sceneIds) {
        List<SceneManageWrapperOutput> byIds = sceneManageService.getByIds(sceneIds);
        return ResponseResult.success(byIds);
    }

    @PostMapping
    @ApiOperation(value = "新增压测场景")
    public ResponseResult<Long> add(@RequestBody @Valid SceneManageWrapperReq wrapperReq) {
        SceneManageWrapperInput input = new SceneManageWrapperInput();
        dataModelConvert(wrapperReq, input);
        Long aLong = sceneManageService.addSceneManage(input);
        return ResponseResult.success(aLong);
    }

    public void dataModelConvert(SceneManageWrapperReq wrapperReq, SceneManageWrapperInput input) {
        BeanUtils.copyProperties(wrapperReq, input);
        input.setStopCondition(SceneSlaRefInputConverter.ofList(wrapperReq.getStopCondition()));
        input.setWarningCondition(SceneSlaRefInputConverter.ofList(wrapperReq.getWarningCondition()));
        input.setBusinessActivityConfig(
            SceneBusinessActivityRefInputConvert.ofLists(wrapperReq.getBusinessActivityConfig()));
        input.setUploadFile(SceneScriptRefInputConvert.ofList(wrapperReq.getUploadFile()));

    }

    @PutMapping
    @ApiOperation(value = "修改压测场景")
    public ResponseResult<String> update(@RequestBody @Valid SceneManageWrapperReq wrapperReq) {
        SceneManageWrapperInput input = new SceneManageWrapperInput();
        dataModelConvert(wrapperReq, input);
        sceneManageService.updateSceneManage(input);
        return ResponseResult.success();
    }

    @DeleteMapping
    @ApiOperation(value = "删除压测场景")
    public ResponseResult<String> delete(@RequestBody SceneManageDeleteReq deleteReq) {
        sceneManageService.delete(deleteReq.getId());
        return ResponseResult.success("删除成功");
    }

    /**
     * 供编辑使用
     */
    @GetMapping("/detail/{id}")
    @ApiOperation(value = "压测场景编辑详情")
    public ResponseResult<SceneManageWrapperResp> getDetailForEdit(@PathVariable("id") Long id) {
        SceneManageQueryOpitons options = new SceneManageQueryOpitons();
        options.setIncludeBusinessActivity(true);
        options.setIncludeScript(true);
        options.setIncludeSLA(true);
        SceneManageWrapperOutput sceneManage = sceneManageService.getSceneManage(id, options);
        wrapperSceneManage(sceneManage);
        SceneManageWrapperResp sceneManageWrapperResp = new SceneManageWrapperResp();
        BeanUtils.copyProperties(sceneManage, sceneManageWrapperResp);
        //TODO 组装扩展字段的数据
        assembleFeatures(sceneManageWrapperResp);
        return ResponseResult.success(sceneManageWrapperResp);

    }

    public void assembleFeatures(SceneManageWrapperResp resp) {
        String features = resp.getFeatures();
        if (StringUtils.isBlank(features)) {
            return;
        }
        HashMap<String, Object> map = JSON.parseObject(features, HashMap.class);
        Integer configType = -1;
        if (map.containsKey(SceneManageConstant.FEATURES_CONFIG_TYPE)) {
            configType = (Integer)map.get(SceneManageConstant.FEATURES_CONFIG_TYPE);
            resp.setConfigType(configType);
        }
        if (map.containsKey(SceneManageConstant.FEATURES_SCRIPT_ID)) {
            Long scriptId = (Long)map.get(SceneManageConstant.FEATURES_SCRIPT_ID);
            // todo 逻辑查看
            if (configType == 1) {
                //业务活动
                List<SceneManageWrapperResp.SceneBusinessActivityRefResp> businessActivityConfig = resp.getBusinessActivityConfig();
                businessActivityConfig.forEach(data -> data.setScriptId(scriptId));
            } else {
                resp.setScriptId(scriptId);
            }
        }
        Object businessFlowId = map.get(SceneManageConstant.FEATURES_BUSINESS_FLOW_ID);
        resp.setBusinessFlowId(businessFlowId == null ? null : (String)businessFlowId);
        if (map.containsKey(SceneManageConstant.FEATURES_SCHEDULE_INTERVAL)) {
            Integer schedualInterval = (Integer)map.get(SceneManageConstant.FEATURES_SCHEDULE_INTERVAL);
            resp.setScheduleInterval(schedualInterval);
        }
    }

    /**
     * 供详情使用
     */
    @GetMapping("/content")
    @ApiOperation(value = "压测场景详情")
    public ResponseResult<SceneDetailResp> getContent(@ApiParam(value = "id") Long id) {
        ResponseResult<SceneManageWrapperResp> resp = getDetailForEdit(id);
        if (!resp.getSuccess()) {
            throw new TakinCloudException(TakinCloudExceptionEnum.SCENE_MANAGE_GET_ERROR, resp.getError().getMsg());
        }
        return ResponseResult.success(convertSceneManageWrapperDTO2SceneDetailDTO(resp.getData()));
    }

    @GetMapping("/list")
    @ApiOperation(value = "压测场景列表")
    public ResponseResult<List<SceneManageListResp>> getList(
        @ApiParam(name = "current", value = "页码", required = true) Integer current,
        @ApiParam(name = "pageSize", value = "页大小", required = true) Integer pageSize,
        @ApiParam(name = "sceneId", value = "压测场景ID") Long sceneId,
        @ApiParam(name = "sceneName", value = "压测场景名称") String sceneName,
        @ApiParam(name = "status", value = "压测状态") Integer status,
        @ApiParam(name = "sceneIds", value = "场景ids，逗号分割") String sceneIds,
        @ApiParam(name = "lastPtStartTime", value = "压测结束时间") String lastPtStartTime,
        @ApiParam(name = "lastPtEndTime", value = "压测结束时间") String lastPtEndTime
    ) {

        /*
         * 1、封装参数
         * 2、调用查询服务
         * 3、返回指定格式
         */
        SceneManageQueryInput queryVO = new SceneManageQueryInput();
        queryVO.setCurrentPage(current);
        queryVO.setPageSize(pageSize);
        queryVO.setSceneId(sceneId);
        queryVO.setSceneName(sceneName);
        queryVO.setStatus(status);
        List<Long> sceneIdList = new ArrayList<>();
        if (StringUtils.isNotBlank(sceneIds)) {
            String[] strList = sceneIds.split(",");
            for (String s : strList) {
                sceneIdList.add(Long.valueOf(s));
            }
        }
        queryVO.setSceneIds(sceneIdList);
        queryVO.setLastPtStartTime(lastPtStartTime);
        queryVO.setLastPtEndTime(lastPtEndTime);
        PageInfo<SceneManageListOutput> pageInfo = sceneManageService.queryPageList(queryVO);
        // 转换
        List<SceneManageListResp> list = pageInfo.getList().stream()
            .map(output -> {
                SceneManageListResp resp = new SceneManageListResp();
                BeanUtils.copyProperties(output, resp);
                return resp;
            }).collect(Collectors.toList());
        return ResponseResult.success(list, pageInfo.getTotal());
    }

    @GetMapping("/listSceneManage")
    @ApiOperation(value = "不分页查询所有场景带脚本信息")
    public ResponseResult<List<SceneManageListResp>> getSceneManageList() {

        List<SceneManageListOutput> sceneManageListOutputs = sceneManageService.querySceneManageList();
        // 转换
        List<SceneManageListResp> list = sceneManageListOutputs.stream()
            .map(output -> {
                SceneManageListResp resp = new SceneManageListResp();
                BeanUtils.copyProperties(output, resp);
                return resp;
            }).collect(Collectors.toList());
        return ResponseResult.success(list);
    }

    @PostMapping("/flow/calc")
    @ApiOperation(value = "预估流量计算")
    public ResponseResult<BigDecimal> calcFlow(@RequestBody SceneManageWrapperReq wrapperReq) {
        SceneManageWrapperInput input = new SceneManageWrapperInput();
        BeanUtils.copyProperties(wrapperReq, input);
        BigDecimal flow = sceneManageService.calcEstimateFlow(input);
        return ResponseResult.success(flow.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * 获取机器数量范围
     *
     * @param concurrenceNum 并发数量
     * @return -
     */
    @GetMapping("/ipnum")
    @ApiOperation(value = "")
    public ResponseResult<StrategyResp> getIpNum(Integer concurrenceNum, Integer tpsNum) {
        StrategyOutputExt output = strategyConfigService.getStrategy(concurrenceNum, tpsNum);
        StrategyResp resp = new StrategyResp();
        BeanUtils.copyProperties(output, resp);
        return ResponseResult.success(resp);
    }

    @PostMapping("/checkAndUpdate/script")
    @ApiOperation(value = "解析脚本")
    public ResponseResult<ScriptCheckResp> checkAndUpdate(@RequestBody ScriptCheckAndUpdateReq req) {
        ScriptVerityRespExt scriptVerityRespExt = sceneManageService.checkAndUpdate(req.getRequest(), req.getUploadPath(),
            req.isAbsolutePath(), req.isUpdate());
        return ResponseResult.success(SceneTaskOpenConverter.INSTANCE.ofScriptVerityRespExt(scriptVerityRespExt));
    }

    private void wrapperSceneManage(SceneManageWrapperOutput sceneManage) {
        if (sceneManage == null || CollectionUtils.isEmpty(sceneManage.getBusinessActivityConfig())) {
            return;
        }
        sceneManage.getBusinessActivityConfig().forEach(data -> {
            if (StringUtils.isBlank(data.getBindRef())) {
                data.setBindRef("-1");
            }
            if (StringUtils.isBlank(data.getApplicationIds())) {
                data.setApplicationIds("-1");
            }
        });
    }

    private SceneDetailResp convertSceneManageWrapperDTO2SceneDetailDTO(SceneManageWrapperResp wrapperResp) {
        SceneDetailResp detailDTO = new SceneDetailResp();
        //基本信息
        detailDTO.setId(wrapperResp.getId());
        detailDTO.setSceneName(wrapperResp.getPressureTestSceneName());
        // 补充插件信息
        CloudPluginUtils.fillCustomerName(wrapperResp, detailDTO);

        detailDTO.setUpdateTime(wrapperResp.getUpdateTime());
        detailDTO.setLastPtTime(wrapperResp.getLastPtTime());
        detailDTO.setStatus(
            dictionaryCache.getObjectByParam(DicKeyConstant.SCENE_MANAGE_STATUS, wrapperResp.getStatus()));
        //业务活动
        if (CollectionUtils.isNotEmpty(wrapperResp.getBusinessActivityConfig())) {
            List<BusinessActivityDetailResp> activity = Lists.newArrayList();
            wrapperResp.getBusinessActivityConfig().forEach(data -> {
                BusinessActivityDetailResp dto = new BusinessActivityDetailResp();
                dto.setBusinessActivityId(data.getBusinessActivityId());
                dto.setBusinessActivityName(data.getBusinessActivityName());
                dto.setTargetTPS(data.getTargetTPS());
                dto.setTargetRT(data.getTargetRT());
                dto.setTargetSuccessRate(data.getTargetSuccessRate());
                dto.setTargetSA(data.getTargetSA());
                activity.add(dto);
            });
            detailDTO.setBusinessActivityConfig(activity);
        }
        //施压配置
        detailDTO.setConcurrenceNum(wrapperResp.getConcurrenceNum());
        detailDTO.setIpNum(wrapperResp.getIpNum());
        detailDTO.setPressureMode(
            dictionaryCache.getObjectByParam(DicKeyConstant.PT_MODEL, wrapperResp.getPressureMode()));
        detailDTO.setPressureTestTime(wrapperResp.getPressureTestTime());
        detailDTO.setIncreasingTime(wrapperResp.getIncreasingTime());
        detailDTO.setStep(wrapperResp.getStep());
        detailDTO.setEstimateFlow(wrapperResp.getEstimateFlow());
        //上传文件
        if (CollectionUtils.isNotEmpty(wrapperResp.getUploadFile())) {
            List<SceneDetailResp.ScriptDetailResp> script = Lists.newArrayList();
            wrapperResp.getUploadFile().forEach(data -> {
                SceneDetailResp.ScriptDetailResp dto = new SceneDetailResp.ScriptDetailResp();
                dto.setFileName(data.getFileName());
                dto.setUploadTime(data.getUploadTime());
                dto.setFileType(dictionaryCache.getObjectByParam(DicKeyConstant.FILE_TYPE, data.getFileType()));
                dto.setUploadedData(data.getUploadedData());
                dto.setIsSplit(dictionaryCache.getObjectByParam(DicKeyConstant.IS_DELETED, data.getIsSplit()));
                script.add(dto);
            });
            detailDTO.setUploadFile(script);
        }

        //SLA配置
        if (CollectionUtils.isNotEmpty(wrapperResp.getStopCondition())) {
            List<SceneDetailResp.SlaDetailResp> sla = Lists.newArrayList();
            wrapperResp.getStopCondition().forEach(data -> {
                SceneDetailResp.SlaDetailResp stop = new SceneDetailResp.SlaDetailResp();
                stop.setRuleName(data.getRuleName());
                stop.setBusinessActivity(
                    convertIdsToNames(data.getBusinessActivity(), detailDTO.getBusinessActivityConfig()));
                stop.setRule(buildRule(data));
                stop.setStatus(dictionaryCache.getObjectByParam(DicKeyConstant.LIVE_STATUS, data.getStatus()));
                sla.add(stop);
            });
            detailDTO.setStopCondition(sla);
        }

        if (CollectionUtils.isNotEmpty(wrapperResp.getWarningCondition())) {
            List<SceneDetailResp.SlaDetailResp> sla = Lists.newArrayList();
            wrapperResp.getWarningCondition().forEach(data -> {
                SceneDetailResp.SlaDetailResp stop = new SceneDetailResp.SlaDetailResp();
                stop.setRuleName(data.getRuleName());
                stop.setBusinessActivity(
                    convertIdsToNames(data.getBusinessActivity(), detailDTO.getBusinessActivityConfig()));
                stop.setRule(buildRule(data));
                stop.setStatus(dictionaryCache.getObjectByParam(DicKeyConstant.LIVE_STATUS, data.getStatus()));
                sla.add(stop);
            });
            detailDTO.setWarningCondition(sla);
        }

        return detailDTO;
    }

    private String buildRule(SceneManageWrapperResp.SceneSlaRefResp slaRefDTO) {
        SceneSlaRefInput input = new SceneSlaRefInput();
        BeanUtils.copyProperties(slaRefDTO, input);
        Map<String, Object> dataMap = SlaUtil.matchCondition(input, new SendMetricsEvent());
        StringBuilder sb = new StringBuilder();
        sb.append(dataMap.get("type"));
        sb.append(dataMap.get("compare"));
        sb.append(slaRefDTO.getRule().getDuring());
        sb.append(dataMap.get("unit"));
        sb.append("连续出现");
        sb.append(slaRefDTO.getRule().getTimes());
        sb.append("次");
        return sb.toString();
    }

    private String convertIdsToNames(String[] ids, List<BusinessActivityDetailResp> detailList) {
        if (ids == null || ids.length == 0 || CollectionUtils.isEmpty(detailList)) {
            return null;
        }
        Map<String, String> detailMap = ListHelper.transferToMap(detailList,
            data -> String.valueOf(data.getBusinessActivityId()), BusinessActivityDetailResp::getBusinessActivityName);

        StringBuffer sb = new StringBuffer();
        for (String id : ids) {
            if ("-1".equals(id)) {
                sb.append("所有");
            } else {
                sb.append(detailMap.get(id));
            }
            sb.append(",");
        }
        sb.deleteCharAt(sb.lastIndexOf(","));
        return sb.toString();
    }

}
