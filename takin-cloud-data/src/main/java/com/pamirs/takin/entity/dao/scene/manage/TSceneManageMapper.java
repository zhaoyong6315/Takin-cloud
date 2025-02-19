package com.pamirs.takin.entity.dao.scene.manage;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import com.pamirs.takin.entity.domain.entity.scene.manage.SceneManage;
import io.shulie.takin.cloud.common.bean.scenemanage.UpdateStatusBean;
import io.shulie.takin.cloud.common.bean.scenemanage.SceneManageQueryBean;
import io.shulie.takin.cloud.common.annotation.DataApartInterceptAnnotation;

@Mapper
@Deprecated
public interface TSceneManageMapper {

    int deleteByPrimaryKey(Long id);

    Long insertSelective(SceneManage record);

    int updateByPrimaryKeySelective(SceneManage record);

    int updateStatus(UpdateStatusBean record);

    @DataApartInterceptAnnotation
    List<SceneManage> getPageList(SceneManageQueryBean queryVO);

    /**
     * 查询所有场景信息
     *
     * @return -
     */
    List<SceneManage> selectAllSceneManageList();

    int resumeStatus(Long id);

    int updateSceneUserById(@Param("id") Long id, @Param("userId") Long userId);

    List<SceneManage> getByIds(@Param("ids") List<Long> ids);

}
