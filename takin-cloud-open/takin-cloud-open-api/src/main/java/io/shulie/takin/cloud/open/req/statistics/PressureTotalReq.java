package io.shulie.takin.cloud.open.req.statistics;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.shulie.takin.ext.content.user.CloudUserCommonRequestExt;

/**
 * @author 无涯
 * @date 2020/11/30 9:23 下午
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PressureTotalReq extends CloudUserCommonRequestExt implements Serializable {
    private String type;
    private String startTime;
    private String endTime;
    /**
     * 压测脚本
     */
    private List<Long> scriptIds;
}
