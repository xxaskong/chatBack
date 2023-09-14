package cn.xk.chatBack.service.Impl;

import cn.xk.chatBack.mapper.GroupMessageMapper;
import cn.xk.chatBack.model.*;
import cn.xk.chatBack.model.connect.ActiveGroupUser;
import cn.xk.chatBack.model.connect.UserSocketPool;
import cn.xk.chatBack.model.constant.RCode;
import cn.xk.chatBack.model.constant.TargetType;
import cn.xk.chatBack.model.message.GroupMessage;
import cn.xk.chatBack.service.GroupMessageService;
import cn.xk.chatBack.service.UserMessageService;
import com.corundumstudio.socketio.SocketIOClient;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-06
 */
@Slf4j
@Service
public class GroupMessageServiceImpl implements GroupMessageService {

    @Resource
    GroupMessageMapper groupMessageMapper;

    @Resource
    UserMessageService userMessageService;

    @Resource
    ActiveGroupUser activeGroupUser;

    @Resource
    UserSocketPool userSocketPool;

    @Override
    public int insertGroupMessage(GroupMessage groupMessage) {
        return groupMessageMapper.insert(groupMessage);
    }

    /**
     * 集群发送消息
     *
     * @param groupMessage
     * @return
     */
    @Override
    public R<?> sendGroupMessage(String groupId, Object groupMessage) {
        //获取在线列表
        ConcurrentMap<String, User> groupMembers = activeGroupUser.getGroupMembers(groupId);
        for (String userId : groupMembers.keySet()) {
            userMessageService.sendMessage(userId, TargetType.GROUP,groupMessage);
        }
        return R.ok();
    }

    /**
     * 群内广播时间
     *
     * @param name
     * @param o
     * @return
     */
    @Override
    public R<?> sendEvent(String name, String groupId, Object o) {

        //获取在线列表
        ConcurrentMap<String, User> groupMembers = activeGroupUser.getGroupMembers(groupId);
        for (String userId : groupMembers.keySet()) {
            userMessageService.sendEvent(name, userId, o);
        }
        return R.ok();
    }
}
