package com.xin.xinpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登陆请求
 */
@Data
public class UserLoginRequest implements Serializable {
    /**
     * 账号
     */
    private String userAccount;
    /**
     * 密码
     */
    private String userPassword;

    private static final long serialVersionUID = 1L;
}
