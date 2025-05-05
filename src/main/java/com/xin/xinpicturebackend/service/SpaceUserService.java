package com.xin.xinpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.xinpicturebackend.model.dto.space.SpaceAddRequest;
import com.xin.xinpicturebackend.model.dto.space.SpaceQueryRequest;
import com.xin.xinpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.xin.xinpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.xin.xinpicturebackend.model.entity.Space;
import com.xin.xinpicturebackend.model.entity.SpaceUser;
import com.xin.xinpicturebackend.model.entity.User;
import com.xin.xinpicturebackend.model.vo.SpaceUserVO;
import com.xin.xinpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 新
* @description 针对表【space_user(空间用户关联表)】的数据库操作Service
* @createDate 2025-05-04 18:00:24
*/
public interface SpaceUserService extends IService<SpaceUser> {
    /**
     * 将空间用户对象转成VO(对信息进行脱敏)
     * @param spaceUser 空间信息
     * @param request 当前请求
     * @return 脱敏后的图像信息
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);
    /**
     * 将空间用户对象转成VO(对信息进行脱敏)
     * @param spaceUserList spaceUser数据
     * @return 空间对象VO数据
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 将空间查询对象转化成queryWrapper对象
     * @param spaceUserQueryRequest 空间查询对象
     * @return   queryWrapper对象
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 校验空间成员
     * @param spaceUser 空间对象
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);


    /**
     * 创建空间成员
     * @param spaceAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceAddRequest);

}
