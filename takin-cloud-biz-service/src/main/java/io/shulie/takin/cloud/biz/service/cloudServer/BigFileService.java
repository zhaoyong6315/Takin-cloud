package io.shulie.takin.cloud.biz.service.cloudServer;

import java.io.File;
import java.util.Map;

import com.pamirs.takin.entity.domain.vo.file.Part;
import io.shulie.takin.common.beans.response.ResponseResult;

/**
 * @author mubai
 * @date 2020-05-12 14:49
 */
public interface BigFileService {

    /**
     * 上传文件
     *
     * @param dto 数据传输对象
     * @return -
     */
    ResponseResult<?> upload(Part dto);

    /**
     * 整合文件形成大文件
     *
     * @param param 参数功能
     * @return -
     */
    ResponseResult<Map<String, Object>> compact(Part param);

    File getPradarUploadFile();

}
