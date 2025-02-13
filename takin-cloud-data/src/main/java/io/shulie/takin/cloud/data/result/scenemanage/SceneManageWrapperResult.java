package io.shulie.takin.cloud.data.result.scenemanage;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import javax.validation.constraints.NotNull;

import io.shulie.takin.cloud.common.bean.TimeBean;
import io.shulie.takin.ext.content.user.CloudUserCommonRequestExt;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author qianshui
 * @date 2020/4/17 下午5:55
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "场景详情出参")
public class SceneManageWrapperResult extends CloudUserCommonRequestExt implements Serializable {

    private static final long serialVersionUID = 7324148443733465383L;

    @ApiModelProperty(value = "压测场景ID")
    private Long id;

    @ApiModelProperty(value = "压测场景名称")
    private String pressureTestSceneName;

    @ApiModelProperty(value = "业务活动配置")
    private List<SceneBusinessActivityRefResult> businessActivityConfig;

    @ApiModelProperty(value = "并发数量")
    private Integer concurrenceNum;

    @ApiModelProperty(value = "指定IP数")
    private Integer ipNum;

    @ApiModelProperty(value = "压测时长(秒)")
    private Long pressureTestSecond;

    @ApiModelProperty(value = "压测时长")
    private TimeBean pressureTestTime;

    @ApiModelProperty(value = "施压模式")
    @NotNull(message = "施压模式不能为空")
    private Integer pressureMode;

    @ApiModelProperty(value = "递增时长(秒)")
    private Long increasingSecond;

    @ApiModelProperty(value = "递增时长")
    private TimeBean increasingTime;

    @ApiModelProperty(value = "阶梯层数")
    private Integer step;

    @ApiModelProperty(value = "预计消耗流量")
    private BigDecimal estimateFlow;

    @ApiModelProperty(name = "scriptType", value = "脚本类型")
    private Integer scriptType;

    @ApiModelProperty(name = "uploadFile", value = "压测脚本/文件")
    private List<SceneScriptRefResult> uploadFile;

    @ApiModelProperty(name = "stopCondition", value = "SLA终止配置")
    private List<SceneSlaRefResult> stopCondition;

    @ApiModelProperty(name = "warningCondition", value = "SLA警告配置")
    private List<SceneSlaRefResult> warningCondition;

    @ApiModelProperty(name = "status", value = "压测状态")
    private Integer status;

    @ApiModelProperty(value = "总测试时长(压测时长+预热时长)")
    private transient Long totalTestTime;

    private transient String updateTime;

    private transient String lastPtTime;
}
