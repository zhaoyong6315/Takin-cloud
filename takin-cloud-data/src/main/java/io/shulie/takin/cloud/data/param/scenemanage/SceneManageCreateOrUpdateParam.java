package io.shulie.takin.cloud.data.param.scenemanage;

import java.util.Date;

import lombok.Data;

/**
 * @author by: hezhongqi
 * @date 2021/8/3 15:06
 */
@Data
public class SceneManageCreateOrUpdateParam {

    private Long id;

    private String sceneName;

    private Integer status;

    private Date lastPtTime;

    private Integer scriptType;

    private Integer type;

    private Integer isDeleted;

    private Date createTime;

    private String features;

    private String createName;

    private Date updateTime;

    private String updateName;

    private String ptConfig;

}
