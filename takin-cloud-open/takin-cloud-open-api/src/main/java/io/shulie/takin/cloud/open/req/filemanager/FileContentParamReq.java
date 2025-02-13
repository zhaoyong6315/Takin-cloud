package io.shulie.takin.cloud.open.req.filemanager;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.shulie.takin.ext.content.user.CloudUserCommonRequestExt;

/**
 * @author 无涯
 * @date 2020/12/9 10:50 上午
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileContentParamReq extends CloudUserCommonRequestExt {
    private List<String> paths;
}
