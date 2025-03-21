package com.xin.xinpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xin.xinpicturebackend.model.dto.user.UserLoginRequest;
import com.xin.xinpicturebackend.model.dto.user.UserQueryRequest;
import com.xin.xinpicturebackend.model.dto.user.UserRegisterRequest;
import com.xin.xinpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.xinpicturebackend.model.vo.LoginUserVO;
import com.xin.xinpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 新
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-03-12 20:51:29
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param userRegisterRequest 用户信息
     * @return 用户id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 获取加密后的密码
     * @param userPassword 密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登陆
     * @param userLoginRequest 用户信息
     * @return 用户页面信息（脱敏）
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 获取脱敏后的用户信息
     * @param user 未脱敏用户信息
     * @return 脱敏后的用户信息
     */
    LoginUserVO getLoginUserVo(User user);

    /**
     * 获取当前登陆的用户
     * @param request 当前请求
     * @return 当前用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取脱敏后的用户信息
     * @param user 未脱敏用户信息
     * @return 当前用户视图
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息列表
     * @param userList 用户信息列表
     * @return 脱敏后的用户信息列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户注销
     * @param request 当前请求
     * @return 操作结果
     */
    Boolean userLogout(HttpServletRequest request);

    /**
     * 获取查询条件
     * @param userQueryRequest 查询条件
     * @return QueryWrapper
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 判断用户是否为管理员
     * @param user 用户
     * @return  true/false
     */
    boolean isAdmin(User user);
}
