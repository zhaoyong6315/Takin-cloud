package io.shulie.takin.cloud.biz.output.scenetask;

import lombok.Data;

/**
 * ClassName:    SceneJobStateOutput
 * Package:    io.shulie.takin.cloud.biz.output.scenetask
 * Description:
 * Datetime:    2021/6/23   下午5:12
 * Author:   chenhongqiao@shulie.com
 */
@Data
public class SceneJobStateOutput {
    /**
     * 状态 未运行：none 运行中：running 运行失败：failed
     */
    private String state;

    /**
     * 描述信息
     */
    private String msg;
}
