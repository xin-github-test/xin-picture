package com.xin.xinpicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import com.xin.xinpicturebackend.manager.auth.SpaceUserAuthManager;
import com.xin.xinpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.xin.xinpicturebackend.model.entity.Picture;
import com.xin.xinpicturebackend.model.entity.Space;
import com.xin.xinpicturebackend.model.entity.User;
import com.xin.xinpicturebackend.model.enums.SpaceTypeEnum;
import com.xin.xinpicturebackend.service.PictureService;
import com.xin.xinpicturebackend.service.SpaceService;
import com.xin.xinpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Websocket 拦截器，在建立连接时进行校验
 */
@Slf4j
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {
    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        //获取当前登陆用户
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            //从请求中获取参数
            String pictureId = servletRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureId)) {
                log.error("缺少图片参数，拒绝握手！");
                return false;
            }
            //获取当前用户
            User loginUser = userService.getLoginUser(servletRequest);
            if (ObjUtil.isEmpty(loginUser)) {
                log.error("用户未登录，拒绝握手！");
                return false;
            }
            //校验用户是否有编辑当前图片的权限
            Picture picture = pictureService.getById(pictureId);
            if (ObjUtil.isEmpty(picture)) {
                log.error("图片不存在，拒绝握手！");
                return false;
            }
            //校验空间
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != 0) {
                space = spaceService.getById(spaceId);
                if (ObjUtil.isEmpty(space)) {
                    log.error("空间不存在，拒绝握手！");
                    return false;
                }
                //空间存在，判断是否为团队空间
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.error("非团队空间，拒绝握手！");
                    return false;
                }
            }
            //spaceId 为 0 ，则为公共空间
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("用户无权限编辑图片，拒绝握手！");
                return false;
            }

            //所有校验通过，可以建立连接
            //（1）设置用户登陆信息等属性到WebSocket会话中
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("pictureId", Long.valueOf(pictureId)); //记得转化为 Long 类型

        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
