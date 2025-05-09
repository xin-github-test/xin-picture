package com.xin.xinpicturebackend.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.xinpicturebackend.annotation.AuthCheck;
import com.xin.xinpicturebackend.common.BaseResponse;
import com.xin.xinpicturebackend.common.DeleteRequest;
import com.xin.xinpicturebackend.common.ResultUtils;
import com.xin.xinpicturebackend.constant.UserConstant;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import com.xin.xinpicturebackend.exception.ThrowUtils;
import com.xin.xinpicturebackend.manager.auth.SpaceUserAuthManager;
import com.xin.xinpicturebackend.model.dto.space.*;
import com.xin.xinpicturebackend.model.entity.Space;
import com.xin.xinpicturebackend.model.entity.User;
import com.xin.xinpicturebackend.model.enums.SpaceLevelEnum;
import com.xin.xinpicturebackend.model.vo.SpaceVO;
import com.xin.xinpicturebackend.service.SpaceService;
import com.xin.xinpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping("/space")
public class SpaceController {
    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    /**
     * 创建空间
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long spaceId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(spaceId);
    }


    /**
     * 更新空间（仅管理员，因为可修改的字段较多）
     * @param spaceUpdateRequest 当前空间
     * @param request 当前请求
     * @return 自定义返回
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //将实体和DTO进行转换
        Space space = new Space();
        BeanUtil.copyProperties(spaceUpdateRequest, space);
        spaceService.fillSpaceBySpaceLevel(space);
        //数据校验
        spaceService.validSpace(space, false);
        //判断是否存在
        Long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(null == oldSpace, ErrorCode.NOT_FOUND_ERROR);
        //操作数据库
        boolean res = spaceService.updateById(space);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR,"空间更新失败！");
        return ResultUtils.success(true);

    }

    /**
     * 根据id查询空间信息（未脱敏版，仅管理员可用）
     * @param id 空间id
     * @param request 当前请求
     * @return 空间信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(long id, HttpServletRequest request) {
        if (ObjectUtil.isEmpty(id) || id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断是否存在
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(null == space, ErrorCode.NOT_FOUND_ERROR, "空间不存在！");
        return ResultUtils.success(space);
    }

    /**
     * 根据id获取空间信息（脱敏版）
     * @param id 空间id
     * @param request 请求
     * @return 空间信息
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        //判断是否存在
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(null == space, ErrorCode.NOT_FOUND_ERROR, "空间不存在！");
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        //返回权限列表
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        spaceVO.setPermissionList(permissionList);
        //返回脱敏后的数据
        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间列表：未脱敏版（仅管理员）
     * @param spaceQueryRequest 查询请求
     * @param request 当前请求
     * @return 空间分页列表
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = spaceQueryRequest.getCurrent();
        int size = spaceQueryRequest.getPageSize();
        //查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size), spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }

    /**
     * 分页获取空间列表：脱敏版
     * @param spaceQueryRequest 查询请求
     * @param request 当前请求
     * @return 空间分页列表
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = spaceQueryRequest.getCurrent();
        int size = spaceQueryRequest.getPageSize();

        //限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size), spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage, request));
    }


    /**
     * 编辑空间（仅本人和管理员）
     * @param spaceEditRequest 新空间的信息
     * @param request 当前请求
     * @return 是否编辑成功
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        if (spaceEditRequest == null || spaceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //参数校验
        Space space = new Space();
        BeanUtil.copyProperties(spaceEditRequest, space);
        //补充字段
        spaceService.fillSpaceBySpaceLevel(space);
        //编辑时间
        space.setEditTime(new Date());
        spaceService.validSpace(space, false);
        //获取当前登陆用户（仅管理员和本人能编辑）
        User loginUser = userService.getLoginUser(request);
        //补充审核字段
        spaceService.fillSpaceBySpaceLevel(space);
        //判断是否存在
        Long id = spaceEditRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        //权限校验(仅本人和管理员能修改)
        spaceService.checkSpaceAuth(loginUser, oldSpace);
        //操作数据库
        boolean res = spaceService.updateById(space);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR,"空间更新失败！");
        return ResultUtils.success(true);
    }
    /**
     * 删除空间（仅本人和管理员）
     * @param deleteRequest 删除的空间信息
     * @param request 请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误异常！");
        }
        //权限校验（由于用户和管理员都能删除空间，因此需要更加细粒度的控制权限）
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);

        //权限校验
        //只有本人和管理员能删除
        spaceService.checkSpaceAuth(loginUser, oldSpace);
        //操作数据库
        boolean res = spaceService.removeById(id);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR, "空间删除失败！");
        return ResultUtils.success(true);
    }

    /**
     * 获取所有空间级别信息，方便前端展示
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                ))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }

}
