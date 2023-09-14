package cn.xk.chatBack.handler;

import cn.xk.chatBack.model.*;
import cn.xk.chatBack.model.connect.ActiveGroupUser;
import cn.xk.chatBack.model.connect.UserSocketPool;
import cn.xk.chatBack.model.constant.RCode;
import cn.xk.chatBack.model.message.ChatGroupMsgList;
import cn.xk.chatBack.model.message.GroupMessage;
import cn.xk.chatBack.model.message.UserMsgList;
import cn.xk.chatBack.service.GroupMessageService;
import cn.xk.chatBack.service.GroupService;
import cn.xk.chatBack.service.UserMessageService;
import cn.xk.chatBack.service.UserService;
import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnEvent;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.*;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-08
 */
@Slf4j
@Component
public class RelationshipHandler {

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
    public UserMessageService userMessageService;

    @Autowired
    public GroupMessageService groupMessageService;


    @OnEvent(value ="activeGroupUser")
    public void activeGroupUser(SocketIOClient client){
        String userId = userSocketPool.getUserId(client.getSessionId().toString());
        groupMessageService.sendEvent("activeGroupUser", "lobby", groupService.selectActiveGroupUser(userId));
    }

    @OnEvent(value = "chatData")
    public void onEvent(SocketIOClient client, User user) {
        log.info("chatData:{}", user);
        Map<String, Object> map = new HashMap<>();

//        groupData:[] 获取所有群聊消息（获取30条历史消息）
        HashSet<Object> userList = new HashSet<>();
        List<ChatGroupMsgList> groupMsgLists = groupService.getGroupChatByUserId(user.getUserId());
        for (ChatGroupMsgList groupMsgList : groupMsgLists) {
            for (GroupMessage message : groupMsgList.getMessages()) {
                String userId = message.getUserId();
                userList.add(userService.selectUserByUserId(userId));
            }
        }
        map.put("groupData", groupMsgLists);

//        获取所有好友名
        List<UserMsgList> userMsgLists = userService.getUserChatByUserId(user.getUserId());
        map.put("friendData", userMsgLists);
        List<Object> userData = new ArrayList<>();
        userData.add(user);
        userData.addAll(userMsgLists);
        map.put("userData", userList);
        client.sendEvent("chatData", new AckCallback<String>(String.class) {
            @Override
            public void onSuccess(String result) {
                System.out.println("ack from client: " + client.getSessionId() + " data: " + result);
            }
        }, R.ok(map));
    }

    @OnEvent(value = "joinGroup")
    public void joinGroup(SocketIOClient client, Map<String, String> requestMap) {
        String userId = requestMap.get("userId");
        String groupId = requestMap.get("groupId");
        User user = client.get("user");
        R<Map<String, Object>> result = new R<>();
        if (!user.getUserId().equals(userId)) {
            result.setCode(RCode.FAIL);
            result.setMsg("加群失败请重试");
            client.sendEvent("joinGroup", result);
            return;
        }
        ChatGroup group = groupService.selectGroupByGroupId(groupId);
        if (ObjectUtils.isEmpty(group)) {
            //群不存在
            result.setCode(RCode.FAIL);
            result.setMsg("群不存在请重试");
            client.sendEvent("joinGroup", result);
            return;
        }
        int count = groupService.joinGroup(userId, groupId);
        //上线该用户并通知
        activeGroupUser.userOnline(groupId, user);
        RWS<?> rws = groupService.selectActiveGroupUser(userId);
        client.joinRoom(groupId);
        //todo 优化通知对象
        groupMessageService.sendEvent("activeGroupUser", "lobby", rws);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("user", user);
        resultMap.put("group", group);
        result.setData(resultMap);
        result.setCode(RCode.OK);
        client.sendEvent("joinGroup", result);
    }

    @OnEvent(value = "addGroup")
    public void addGroup(SocketIOClient client, Map<String, String> requestMap) {
        String userId = requestMap.get("userId");
        String groupName = requestMap.get("groupName");
        User user = client.get("user");
        long createTime = Long.parseLong(String.valueOf(requestMap.get("createTime")));
        //判断群名是否重复
        ChatGroup group = groupService.selectGroupByGroupName(groupName);
        //如果查出的group不为空则存在该群
        if (!ObjectUtils.isEmpty(group)) {
            R<ChatGroup> result = new R<>();
            result.setCode(R.FAIL);
            result.setMsg("该群聊已经存在");
            result.setData(group);
            client.sendEvent("addGroup", result);
            return;
        }
        //创建群对象并插入
        group = new ChatGroup();
        group.setUserId(userId);
        group.setGroupName(groupName);
        group.setNotice("群主很懒,没有设置群公告");
        group.setCreateTime(System.currentTimeMillis());
        int count = groupService.insertGroup(group);
        //将插入的群加入缓存
        activeGroupUser.groupOnline(group.getGroupId());
        //将当前用户加入该群聊并上线
        groupService.joinGroup(userId, group.getGroupId());
        activeGroupUser.userOnline(group.getGroupId(), user);
        //刷新群在线
        RWS<?> rws = groupService.selectActiveGroupUser(userId);
        BroadcastOperations lobby = socketIoServer.getRoomOperations("lobby");
        R<Object> ok = R.ok();
        ok.setCode(0);
        ok.setMsg("创建成功");
        ok.setData(group);
        client.sendEvent("addGroup", ok);
        groupMessageService.sendEvent("activeGroupUser", "lobby", rws);
    }

    @OnEvent(value = "exitGroup")
    public void exitGroup(SocketIOClient client, Map<String, String> requestMap) {
        String userId = requestMap.get("userId");
        String groupId = requestMap.get("groupId");
        User user = client.get("user");
        R<Map<String, String>> result = new R<>();
        if (!user.getUserId().equals(userId)) {
            result.setCode(RCode.FAIL);
            result.setMsg("加群失败请重试");
            client.sendEvent("joinGroup", result);
            return;
        }
        ChatGroup group = groupService.selectGroupByGroupId(groupId);
        if (ObjectUtils.isEmpty(group)) {
            //群不存在
            result.setCode(RCode.FAIL);
            result.setMsg("群不存在请重试");
            client.sendEvent("joinGroup", result);
            return;
        }

        int count = groupService.exitGroup(userId, groupId);
        if (count > 0) {
            client.leaveRoom(groupId);
            activeGroupUser.userOffline(groupId, user);
            RWS<?> rws = groupService.selectActiveGroupUser(userId);
            //todo 优化通知对象 这段代码进行封装
            groupMessageService.sendEvent("activeGroupUser", "lobby", rws);
            result.setData(requestMap);
            client.sendEvent("exitGroup", result);
        } else {
            result.setCode(RCode.FAIL);
            result.setMsg("退群失败请重试");
            client.sendEvent("exitGroup", result);
        }

    }

    @OnEvent(value = "addFriend")
    public void addFriend(SocketIOClient client, @NotNull Map<String, String> requestMap) {
        String userId = requestMap.get("userId");
        String friendId = requestMap.get("friendId");
        long createTime = Long.parseLong(String.valueOf(requestMap.get("createTime")));
        User friend = userService.selectUserByUserId(friendId);
        R<User> r = new R<>();
        //判断好友是否存在
        if (ObjectUtils.isEmpty(friend)) {
            r.setCode(1);
            r.setMsg("好友不存在");
            client.sendEvent("addFriend", r);
            return;
        }
        //判断好友id是否是自己
        if (userId.equals(friendId)) {
            r.setCode(R.FAIL);
            r.setMsg("不能加自己为好友");
            client.sendEvent("addFriend", r);
            return;
        }
        //判断好友是否已经存在
        if (userService.friendExist(userId, friendId) > 0) {
            r.setCode(1);
            r.setMsg("好友已存在不能重复添加");
            client.sendEvent("addFriend", r);
        }
        //双方加好友
        int result = userService.addFriend(userId, friendId);
        User user = client.get("user");
        if (Objects.isNull(user)) {
            user = userService.selectUserByUserId(userId);
        }
        r.setCode(0);
        r.setMsg("添加成功");
        r.setData(friend);
        client.sendEvent("addFriend", r);
//        List<String> friendIdList = client.get("friendIdList");
//        friendIdList.add(friendId);
        r.setMsg(user.getUserName() + "请求添加好友");
        r.setData(user);
        userMessageService.sendEvent("addFriend", friendId, r);
    }

    @OnEvent(value = "exitFriend")
    public void exitFriend(SocketIOClient client, Map<String, String> requestMap) {
        String userId = requestMap.get("userId");
        String friendId = requestMap.get("friendId");
        User user = client.get("user");
        R<Map<String, String>> result = new R<>();
        if (!user.getUserId().equals(userId)) {
            result.setCode(R.FAIL);
            result.setMsg("删除失败请重试");
            client.sendEvent("exitFriend", result);
            return;
        }
        User friend = userService.selectUserByUserId(friendId);
        if (ObjectUtils.isEmpty(friend)) {
            result.setCode(R.FAIL);
            result.setMsg("删除失败好友不存在请重试");
            client.sendEvent("exitFriend", result);
            return;
        }
        int count = userService.exitFriend(userId, friendId);
        if (count > 0) {
            List<String> friendIdList = client.get("friendIdList");
            //todo 可靠性保证
            boolean remove = friendIdList.remove(friendId);
            result.setCode(RCode.OK);
            result.setMsg("删除成功");
            result.setData(requestMap);
            client.sendEvent("exitFriend", result);
        } else {
            result.setCode(RCode.FAIL);
            result.setMsg("删除失败请重试");
            client.sendEvent("exitFriend", result);
        }
    }

}
