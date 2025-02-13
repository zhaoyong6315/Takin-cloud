package io.shulie.takin.ext.content.enginecall;


import lombok.Data;

/**
 * @author 莫问
 * @date 2020-05-12
 */
@Data
public class ScheduleRunRequest {

    /**
     * 调度ID
     */
    private Long scheduleId;

    /**
     * 调度参数
     */
    private ScheduleStartRequestExt request;

    /**
     * 策略配置
     */
    private StrategyConfigExt strategyConfig;
}
