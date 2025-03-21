package com.xin.xinpicturebackend.constant;

/**
 * 用户常量
 */
public interface UserConstant {
    /**
     * 用户登陆态键
     */
    String USER_LOGIN_STATE = "user_login";
    /**
     * 用户默认密码
     */
    String DEFAULT_PASSWORD = "xin123456";
    //region 权限
    /**
     * 盐（加密）
     */
    String SALT = "xin666";
    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";
    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    //endregion
}
