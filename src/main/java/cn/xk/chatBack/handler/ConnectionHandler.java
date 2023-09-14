package cn.xk.chatBack.handler;

import cn.xk.chatBack.model.*;
import cn.xk.chatBack.model.connect.ActiveGroupUser;
import cn.xk.chatBack.model.connect.UserSocketPool;
import cn.xk.chatBack.service.GroupMessageService;
import cn.xk.chatBack.service.GroupService;
import cn.xk.chatBack.service.UserMessageService;
import cn.xk.chatBack.service.UserService;
import cn.xk.chatBack.utils.jwt.JwtUtil;
import com.alibaba.fastjson.JSON;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-08
 */
@Slf4j
@Component
public class ConnectionHandler {

    @Autowired
    private SocketIOServer socketIoServer;

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    //群里在线的群成员
    @Autowired
    private ActiveGroupUser activeGroupUser;

    @Autowired
    public UserSocketPool userSocketPool;

    @Autowired
    UserMessageService userMessageService;

    @Autowired
    GroupMessageService groupMessageService;

    @Value("${spring.application.name}")
    private String serverName;

    @Autowired
    private HazelcastInstance hazelcastInstance;



    /**
     * 客户端连接的时候触发
     *
     * @param client
     */
    @OnConnect
    public void onConnect(SocketIOClient client) {
//        String token = client.getHandshakeData().getSingleUrlParam("token");
//        //解析token
//        String json;
//        String id = null;
//        try {
//            Claims claims = JwtUtil.parseJWT(token);
//            json = claims.getSubject();
//            Map<String, Object> map = JSON.parseObject(json, Map.class);
//            id = (String) map.get("id");
//        } catch (Exception e) {
//            client.sendEvent("tokenExpiration", "");
//            log.error("token非法:{}", e.getMessage());
//        }
        //存储SocketIOClient
        String id = client.getHandshakeData().getSingleUrlParam("id");
        if (StringUtils.hasText(id)) {
            userSocketPool.addSocket(id, client);
            log.info("客户端:{},已连接id:{}", client.getSessionId(), id);
            //加入默认群
            User user = userService.selectUserByUserId(id);
            userSocketPool.addSession(client.getSessionId().toString(), user.getUserId());
            client.set("user", user);
            List<String> friendIdList = userService.getFriendIdList(user.getUserId());
            client.set("friendIdList", friendIdList);
            // key 为userid value为所在服务
            IMap<Object, Object> userOnlineMap = hazelcastInstance.getMap("userOnlineMap");
            userOnlineMap.put(user.getUserId(), serverName);
            //上线大厅
            activeGroupUser.userOnline("lobby", user);
//            RWS<?> rws = groupService.selectActiveGroupUser(id);

            log.info("userId:{}",user.getUserId());
            log.info("登录广播{}:",id);
            log.info("登录状态:{}",client.isChannelOpen());
            ConcurrentMap<String, User> groupMembers = activeGroupUser.getGroupMembers("lobby");
            log.info("大厅人数:{}", groupMembers.keySet().size());
        } else {
            client.sendEvent("tokenExpiration", "");
            log.error("websocket连接失败id为空");
        }
    }


    /**
     * 客户端关闭连接时触发
     *
     * @param client
     */
    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        //在client缓存中拿到user
        String userId = userSocketPool.getUserId(client.getSessionId().toString());

        if (!Objects.isNull(userId)){
            activeGroupUser.userOfflineAll(userId);
            ConcurrentMap<String, User> groupMembers = activeGroupUser.getGroupMembers("lobby");
            RWS<?> rws = groupService.selectActiveGroupUser(userId);
            //rws.setMsg("alldis:"+groupMembers.keySet().size());
            groupMessageService.sendEvent("activeGroupUser", "lobby", rws);

            IMap<Object, Object> userOnlineMap = hazelcastInstance.getMap("userOnlineMap");
            log.info("result:{}",userOnlineMap.size());
            userOnlineMap.remove(userId);
        }

        log.info("客户端:" + client.getSessionId() + "断开连接");
    }

    //joinFriendSocket 将当前用户加入好友socket
    @OnEvent(value = "joinFriendSocket")
    public void joinFriendSocket(SocketIOClient client, Map<String, String> requestMap) {
        String userId = requestMap.get("userId");
        String friendId = requestMap.get("friendId");
        if (!StringUtils.hasText(userId)||!StringUtils.hasText(friendId)){
            return;
        }

        UserRelationship userRelationship = userService.selectUserRelationship(userId, friendId);
        R<UserRelationship> result = R.ok(userRelationship);
        result.setCode(0);
        client.sendEvent("joinFriendSocket", result);
    }

    //joinGroupSocket 将当前用户加入群组socket 发送消息相当于群组内广播
    @OnEvent(value = "joinGroupSocket")
    public void onEvent(SocketIOClient client, Map<String, String> requestMap) {
        log.info("joinGroupSocket  groupId:{} userId:{}", requestMap.get("groupId"), requestMap.get("userId"));
        //查user
        String userId = requestMap.get("userId");
        User user = userService.selectUserByUserId(userId);
        if (!Objects.isNull(user)) {
            user.setPassWord("");
        }
        //查group
        String groupId = requestMap.get("groupId");
        ChatGroup group = groupService.selectGroupByGroupId(groupId);
        Map<String, Object> map = new HashMap<>();
        map.put("user", user);
        map.put("group", group);
        R<Map<String, Object>> result = R.ok(map);
        result.setCode(0);
        //将当前用户加入群组socket
        client.sendEvent("joinGroupSocket", result);
    }
}
