package com.xin.xinpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import com.xin.xinpicturebackend.exception.ThrowUtils;
import com.xin.xinpicturebackend.manager.sharding.DynamicShardingManager;
import com.xin.xinpicturebackend.mapper.SpaceMapper;
import com.xin.xinpicturebackend.model.dto.space.SpaceAddRequest;
import com.xin.xinpicturebackend.model.dto.space.SpaceQueryRequest;
import com.xin.xinpicturebackend.model.entity.Space;
import com.xin.xinpicturebackend.model.entity.SpaceUser;
import com.xin.xinpicturebackend.model.entity.User;
import com.xin.xinpicturebackend.model.enums.SpaceLevelEnum;
import com.xin.xinpicturebackend.model.enums.SpaceRoleEnum;
import com.xin.xinpicturebackend.model.enums.SpaceTypeEnum;
import com.xin.xinpicturebackend.model.vo.SpaceVO;
import com.xin.xinpicturebackend.model.vo.UserVO;
import com.xin.xinpicturebackend.service.SpaceService;
import com.xin.xinpicturebackend.service.SpaceUserService;
import com.xin.xinpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 新
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-03-30 16:34:51
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{
    
    @Resource
    UserService userService;
    @Resource
    SpaceUserService spaceUserService;
    @Resource
    TransactionTemplate transactionTemplate;
    @Resource
    @Lazy
    DynamicShardingManager dynamicShardingManager;
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        //对象封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        //关联查询用户信息
        Long userId = space.getUserId();
        if (null != userId && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        //对象列表 => 对象VO列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        //1.关联查询用户信息
        //收集picturList中所涉及的所有userId并去重
        Set<Long> userIdSet= spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        //获取 userId:User 的 Map
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));

        //2.为spaceVO填充user信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        //从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        queryWrapper.eq(ObjectUtil.isNotEmpty(id), "id",id);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceType), "spaceType", spaceType);

        //排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        //校验spaceType
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        // 要创建
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            //创建时必须填写 spaceTypeEnum
            if (spaceTypeEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类别不能为空");
            }
        }
        // 修改数据时，如果要改空间级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        //修改时，也对空间类别进行校验
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类别不存在");
        }
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1. 填充参数默认值
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        //给空间类别设置默认值（私有空间）
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        //填充容量和大小
        this.fillSpaceBySpaceLevel(space);

        //2.校验参数
        this.validSpace(space, true);

        //3.权限校验
        Long userId = loginUser.getId();
        space.setUserId(userId);
        //创建的空间级别不是普通，且创建人也不是管理员，则提示权限不足
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限创建该级别空间！");
        }
        //4.控制同一个用户只能创建一个空间, 以及一个团队空间
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            //手动控制事务
            Long newSpaceId = transactionTemplate.execute(status -> {
                //判断是否已经创建了空间
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();
                //已经创建，则不能再创建了
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间只能创建一个！");
                //创建
                boolean res = this.save(space);
                ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR, "保存空间失败！");
                //若是创建团队空间，则需要额外再补充一条成员记录
                if (SpaceTypeEnum.TEAM.getValue() == space.getSpaceType()) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    //空间创建者默认为管理员
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    res = spaceUserService.save(spaceUser);
                    ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败！");
                }
                // 创建分表（仅对团队空间生效）
                dynamicShardingManager.createSpacePictureTable(space);
                //返回id
                return space.getId();
            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }

    /**
     * 校验空间权限
     * @param loginUser
     * @param space
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        //仅本人和管理可编辑
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限！");
        }
    }

}




