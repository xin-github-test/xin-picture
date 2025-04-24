package com.xin.xinpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.xinpicturebackend.model.dto.space.SpaceAddRequest;
import com.xin.xinpicturebackend.model.dto.space.SpaceQueryRequest;
import com.xin.xinpicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.xinpicturebackend.model.entity.User;
import com.xin.xinpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 新
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-03-30 16:34:51
*/
public interface SpaceService extends IService<Space> {
    /**
     * 将空间对象转成VO(对空间信息进行脱敏)
     * @param space 空间信息
     * @param request 当前请求
     * @return 脱敏后的图像信息
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);
    /**
     * 将空间对象转成VO(对信息进行脱敏)（分页数据）
     * @param spacePage space分页数据
     * @param request 请求
     * @return 空间对象VO分页数据
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 将空间查询对象转化成queryWrapper对象
     * @param spaceQueryRequest 空间查询对象
     * @return   queryWrapper对象
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);
    /**
     * 对空间参数进行校验
     * @param space 空间对象
     */
    void validSpace(Space space, boolean add);

    /**
     * 按照level给space分配容量，可存图片数量等参数
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 创建空间
     * @param spaceAddRequest
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间权限
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);
}
