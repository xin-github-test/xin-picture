package com.xin.xinpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.xinpicturebackend.constant.UserConstant;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import com.xin.xinpicturebackend.exception.ThrowUtils;
import com.xin.xinpicturebackend.manager.auth.StpKit;
import com.xin.xinpicturebackend.model.dto.user.UserLoginRequest;
import com.xin.xinpicturebackend.model.dto.user.UserQueryRequest;
import com.xin.xinpicturebackend.model.dto.user.UserRegisterRequest;
import com.xin.xinpicturebackend.model.entity.User;
import com.xin.xinpicturebackend.model.enums.UserRoleEnum;
import com.xin.xinpicturebackend.model.vo.LoginUserVO;
import com.xin.xinpicturebackend.model.vo.UserVO;
import com.xin.xinpicturebackend.service.UserService;
import com.xin.xinpicturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 新
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-03-12 20:51:29
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    /**
     * 用户注册
     * @param userRegisterRequest  用户信息
     * @return 用户id
     */
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        //0. 获取用户信息
        String userAccount = userRegisterRequest.getUserAccount();
        String password = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        //1. 校验参数
        if (StrUtil.hasBlank(userAccount, password, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空！");
        }
        if (userAccount.length() < 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短！");
        }
        if (password.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短！");
        }
        if (!password.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致！");
        }
        //2. 检查用户账号是否和数据库中的重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long count = this.baseMapper.selectCount(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号重复！");
        //3. 对密码进行加密撒盐
        String encryptPassword = getEncryptPassword(password);
        //4. 插入数据到数据库中
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("xin-star");
        user.setUserRole(UserRoleEnum.User.getValue());
        boolean res = this.save(user);
        ThrowUtils.throwIf(!res, ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误！");
        return user.getId();
    }

    /**
     * 加密
     * @param userPassword 密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        //加盐，混淆密码
        return DigestUtils.md5DigestAsHex((UserConstant.SALT + userPassword).getBytes());
    }

    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        String userAccount = userLoginRequest.getUserAccount();
        String password = userLoginRequest.getUserPassword();
        //1.校验
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, password), ErrorCode.PARAMS_ERROR, "参数为空！");
        if (userAccount.length() < 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号错误！");
        }
        if (password.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码错误！");
        }
        //2.对用户密码进行加密
        String encryptPassword = getEncryptPassword(password);
        //3.查询数据库中的用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        if (null == user) {
            log.info("user login failed, userAccount cannot match userPassword!");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误！");
        }
        //4.保存用户的登陆态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        //使用Sa-Token后，需要将登陆态往Sa-Token中保存一份(用于空间鉴权)
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVo(user);
    }

    /**
     * 获取脱敏后的用户信息
     * @param user 未脱敏用户信息
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getLoginUserVo(User user) {
        if (null == user) {
            return null;
        }
        LoginUserVO loginUserVo = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVo);
        return loginUserVo;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        //判断是否登陆
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (null == currentUser || null == currentUser.getId()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //此处可能出现的问题：用户在前端修改了用户信息，但是session并没有及时更新，导致数据不一致
        //因此此处需要再从数据库中查询一次（追求性能的话，可以不需要）
        currentUser = this.getById(currentUser.getId());
        //可能在用户在操作的过程中，管理员将用户的账号删除了，此时也是出现缓存与数据库不一致的情况
        ThrowUtils.throwIf(null == currentUser, ErrorCode.NOT_LOGIN_ERROR);

        return currentUser;
    }

    /**
     * 获取脱敏后的用户信息
     * @param user 未脱敏的用户信息
     * @return 敏后的用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
        if (null == user) {
            return null;
        }
        UserVO userVo = new UserVO();
        BeanUtil.copyProperties(user, userVo);
        return userVo;
    }

    /**
     * 获取脱敏后的用户信息列表
     * @param userList 用户信息列表
     * @return 脱敏后的用户信息列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public Boolean userLogout(HttpServletRequest request) {
        //判断是否登陆
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (null == userObj) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"未登录");
        }
        //移除登陆态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取查询条件
     * @param userQueryRequest 查询条件
     * @return QueryWrapper
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (null == userQueryRequest) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空！");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);

        return queryWrapper;
    }

    /**
     * 判断用户是否为管理员
     * @param user 用户
     * @return true/false
     */
    @Override
    public boolean isAdmin(User user) {

        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
}




