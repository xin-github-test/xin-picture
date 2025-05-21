package com.xin.xinpicturebackend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.xin.xinpicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.xin.xinpicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.xin.xinpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.xin.xinpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.xin.xinpicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.xin.xinpicturebackend.model.entity.User;
import com.xin.xinpicturebackend.model.vo.UserVO;
import com.xin.xinpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {
    @Resource
    UserService userService;
    @Resource
    PictureEditEventProducer pictureEditEventProducer;
    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 建立连接后处理逻辑
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        //保存会话到集合中
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);
        //构造响应，发送加入编辑的消息通知
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 加入编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        //广播给所有用户
        broadcastToPicture(pictureId, pictureEditResponseMessage);

    }

    /**
     * 收到消息后的处理的逻辑
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        //获取消息内容，将 Json -> PictureEditResponseMessage 对象
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        //从 session 属性中获取到公共参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");

        //根据消息类型处理消息（生产消息 到 disruptor 队列中）
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);

    }

    /**
     * 进入编辑状态（同一时间只允许一个用户进入编辑状态）
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        // 没有用户正在编辑该图片，才能进入编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            //设置当前用户为正在编辑该图片
            pictureEditingUsers.put(pictureId, user.getId());
            //构造响应，发送进入编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("用户 %s 开始编辑图片", user.getUserName());
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            pictureEditResponseMessage.setMessage(message);
            //广播给所有用户
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 获取当前正在编辑的用户
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleGetEditingUserMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        // 正在编辑的用户
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null) {
            User editingUser = userService.getById(editingUserId);
            if (editingUser != null) {
                //直接返回，不需要广播
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.GET_EDITING_USER.getValue());
                String message = String.format("用户 %s 正在编辑", user.getUserName());
                pictureEditResponseMessage.setUser(userService.getUserVO(editingUser));
                pictureEditResponseMessage.setMessage(message);

                //配置 jackson 序列化器
                ObjectMapper objectMapper = new ObjectMapper();
                SimpleModule module = new SimpleModule();
                //配置序列化器，将 Long 类型转化为 String 类型返回给前端,解决前端处理 Long 类型精度丢失问题
                module.addSerializer(Long.class, ToStringSerializer.instance);
                module.addSerializer(Long.TYPE, ToStringSerializer.instance);
                objectMapper.registerModule(module);

                //使用 jackson 序列化器序列化对象为 json 字符串
                String responseMsg = objectMapper.writeValueAsString(pictureEditResponseMessage);
                TextMessage textMessage = new TextMessage(responseMsg);
                session.sendMessage(textMessage);

            }
        }
    }

    /**
     * 编辑操作
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        // 正在编辑的用户
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            log.error("无效的编辑动作！");
            return;
        }
        //确认是当前的编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            //构造响应，发送具体操作的通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("用户 %s 执行了 %s 操作！", user.getUserName(), actionEnum.getText());
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            //广播给所有用户,除了当前正在编辑的用户
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }
    public void handleSavePictureMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        // 正在编辑的用户
        Long editingUserId = pictureEditingUsers.get(pictureId);

        //确认是当前的编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            //构造响应，发送具体操作的通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.SAVE_ACTION.getValue());
            String message = String.format("用户 %s 保存了图片,请重新开始编辑！", user.getUserName());
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            pictureEditResponseMessage.setMessage(message);
            //广播给所有用户,除了当前正在编辑的用户
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }
    /**
     * 退出编辑状态
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        // 正在编辑的用户
        Long editingUserId = pictureEditingUsers.get(pictureId);
        //确认是当前的编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            //移除用户正在编辑该图片
            pictureEditingUsers.remove(pictureId);
            //构造响应，发送退出编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("用户 %s 退出编辑图片", user.getUserName());
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            pictureEditResponseMessage.setMessage(message);
            //广播给所有用户
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 关闭连接(不要通过要关闭的session发送消息)
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        //从 session 中获取到公共参数
        User user = (User)session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");

        //删除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            //删除参与该图片编辑的一个会话
            sessionSet.remove(session);
            //若是整个会话集合为空，则从移除该图片会话集合
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
        //判断当前是否为正在编辑的用户，是的话需要清除正在编辑的状态
        Long editingUserId = pictureEditingUsers.get(pictureId);
        UserVO editingUser = null;
        if (editingUserId != null) {
            editingUser = userService.getUserVO(userService.getById(editingUserId));
            if (editingUserId.equals(user.getId())) {
                //移除用户正在编辑该图片
                pictureEditingUsers.remove(pictureId);
            }
        }
        // 通知其他用户，该用户已经退出编辑会话
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_SESSION.getValue());
        String message = String.format("用户 %s 退出会话", user.getUserName());
        pictureEditResponseMessage.setUser(editingUser);
        pictureEditResponseMessage.setMessage(message);
        //广播给所有用户(用户退出会话)
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 将操作广播给该图片的所有用户(支持排除掉某个 Session)
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession  排除的 session (用户自己)
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {

            //配置 jackson 序列化器
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            //配置序列化器，将 Long 类型转化为 String 类型返回给前端,解决前端处理 Long 类型精度丢失问题
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);

            //使用 jackson 序列化器序列化对象为 json 字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessionSet) {
                //排除掉的 session 不发送
                if (session.equals(excludeSession)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 将操作广播给该图片的所有用户
     * @param pictureId
     * @param pictureEditResponseMessage
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws IOException {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }
}
