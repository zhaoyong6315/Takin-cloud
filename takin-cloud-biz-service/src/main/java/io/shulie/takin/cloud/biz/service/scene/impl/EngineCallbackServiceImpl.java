package io.shulie.takin.cloud.biz.service.scene.impl;

import com.pamirs.takin.entity.domain.vo.engine.EngineNotifyParam;
import com.pamirs.takin.entity.domain.vo.scenemanage.SceneManageStartRecordVO;
import io.shulie.takin.cloud.biz.service.report.ReportService;
import io.shulie.takin.cloud.biz.service.scene.EngineCallbackService;
import io.shulie.takin.cloud.biz.service.scene.SceneManageService;
import io.shulie.takin.cloud.biz.service.scene.SceneTaskService;
import io.shulie.takin.cloud.common.constants.ReportConstans;
import io.shulie.takin.cloud.common.constants.ScheduleConstants;
import io.shulie.takin.cloud.common.constants.SceneTaskRedisConstants;
import io.shulie.takin.cloud.common.enums.engine.EngineStatusEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneRunTaskStatusEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.shulie.takin.utils.json.JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author 何仲奇
 * @date 2020/9/23 2:59 下午
 */
@Service
@Slf4j
public class EngineCallbackServiceImpl implements EngineCallbackService {

    @Autowired
    private RedisClientUtils redisClientUtils;

    @Autowired
    private SceneManageService sceneManageService;

    @Autowired
    private SceneTaskService sceneTaskService;

    @Autowired
    private ReportService reportService;

    @Override
    public ResponseResult<?> notifyEngineState(EngineNotifyParam notify) {
        String engineName = ScheduleConstants.getEngineName(notify.getSceneId(), notify.getResultId(), notify.getCustomerId());
        String scheduleName = ScheduleConstants.getScheduleName(notify.getSceneId(), notify.getResultId(),
            notify.getCustomerId());
        EngineStatusEnum engineStatusEnum = EngineStatusEnum.getEngineStatusEnum(notify.getStatus());
        if (engineStatusEnum == null) {
            log.warn("没有找到引擎回传状态所对应的枚举，回传状态为:{}", notify.getStatus());
            return ResponseResult.success();
        }
        switch (engineStatusEnum) {
            case START_FAILED:
                log.info("本次压测{}-{}-{},压力引擎 启动失败：{},返回参数：{}", notify.getSceneId(), notify.getResultId(),
                    notify.getCustomerId(), notify.getMsg(), JsonHelper.bean2Json(notify));
                // 记录压测引擎 压力引擎 相关错误  这里给出具体哪个 压力节点 调用压力引擎 失败 todo 之后可以指定到具体的压力节点
                String tempFailSign = ScheduleConstants.TEMP_FAIL_SIGN + engineName;
                Long startFailCount = redisClientUtils.increment(tempFailSign, 1);
                // 记录失败原因，成功则不记录报告中 报告直接完成
                reportService.updateReportFeatures(notify.getResultId(), ReportConstans.FINISH_STATUS, ReportConstans.PRESSURE_MSG, notify.getMsg());

                // 如果 这个失败等于 压力节点 数量 则 将本次压测至为失败
                int podTotal = Integer.parseInt(
                    redisClientUtils.getString(ScheduleConstants.getPressureNodeTotalKey(notify.getSceneId(), notify.getResultId(),
                        notify.getCustomerId())));
                if (podTotal <= startFailCount) {
                    sceneManageService.reportRecord(SceneManageStartRecordVO.build(notify.getSceneId(),
                        notify.getResultId(),
                        notify.getCustomerId()).success(false).errorMsg("").build());
                }
                //修改缓存压测启动状态为失败
                setTryRunTaskInfo(notify.getSceneId(), notify.getResultId(), notify.getCustomerId(), notify.getMsg());
                break;
            case INTERRUPT:
                //获取中断状态
                boolean interruptFlag = Boolean.parseBoolean(redisClientUtils.getString(ScheduleConstants.INTERRUPT_POD + scheduleName));
                return ResponseResult.success(interruptFlag);
            case INTERRUPT_SUCCESSED:
                // 中断成功
                log.info("本次压测{}-{}-{} 中断成功", notify.getSceneId(), notify.getResultId(), notify.getCustomerId());
                Long interruptSuccessCount = redisClientUtils.increment(ScheduleConstants.INTERRUPT_POD_NUM + engineName, 1);
                if (interruptFinish(engineName, interruptSuccessCount)) {
                    redisClientUtils.del(ScheduleConstants.INTERRUPT_POD_NUM + scheduleName, ScheduleConstants.INTERRUPT_POD + scheduleName);
                }
                break;
            case INTERRUPT_FAILED:
                // 中断失败
                log.info("本次压测{}-{}-{} 中断失败", notify.getSceneId(), notify.getResultId(), notify.getCustomerId());
                break;
            default: {}
        }
        return ResponseResult.success();
    }

    private boolean interruptFinish(String engineName, Long interruptSuccessCount) {
        boolean redisNotHasKey = !redisClientUtils.hasKey(engineName);
        Long redisValue = Long.valueOf(redisClientUtils.getString(engineName));
        // 解决pod 没有发送事件问题
        return redisNotHasKey
            || interruptSuccessCount.equals(redisValue);
    }

    private void setTryRunTaskInfo(Long sceneId, Long reportId, Long customerId, String errorMsg) {
        log.info("压测启动失败--sceneId:【{}】,reportId:【{}】,customerId:【{}】,errorMsg:【{}】"
            , sceneId, reportId, customerId, errorMsg);
        String tryRunTaskKey = String
            .format(SceneTaskRedisConstants.SCENE_TASK_RUN_KEY + "%s_%s", sceneId, reportId);
        redisClientUtils.hmset(tryRunTaskKey,
            SceneTaskRedisConstants.SCENE_RUN_TASK_STATUS_KEY, SceneRunTaskStatusEnum.FAILED.getText());
        redisClientUtils.hmset(tryRunTaskKey,
            SceneTaskRedisConstants.SCENE_RUN_TASK_ERROR, errorMsg);
        //试跑失败，停掉pod
        sceneTaskService.stop(sceneId);
    }

}
