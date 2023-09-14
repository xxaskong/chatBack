package cn.xk.chatBack.model.connect;

import cn.xk.chatBack.model.User;
import cn.xk.chatBack.service.GroupService;
import com.corundumstudio.socketio.SocketIOServer;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-08-28
 */


@Slf4j
public class ActiveGroupUser {

    @Resource
    private SocketIOServer socketIOServer;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Resource
    GroupService groupService;

    @PostConstruct
    public void init() {
        log.info("activeGroupUser线程名称:{}", Thread.currentThread().getId());
        //换数据底层
        hazelcastInstance.getMap("activeGroupUser");
        //读取所有群初始化map
        List<String> groupIdList = groupService.selectAllGroupId();
        for (int i = 0; i < groupIdList.size(); i++) {
            String groupId = groupIdList.get(i);
            groupOnline(groupId);
        }
    }

    public IMap<String, ConcurrentHashMap<String, User>> getActiveGroupUser(){
        return hazelcastInstance.getMap("activeGroupUser");
    }

    /**
     * 群上线
     *
     * @param groupId
     */
    public void groupOnline(String groupId) {
        IMap<String, ConcurrentHashMap<String, User>> activeGroupUser = getActiveGroupUser();
        activeGroupUser.put(groupId, new ConcurrentHashMap<>());
    }

    /**
     * 广播上线 创建群聊的时候使用
     * 告诉其他集群群创建保证一致性
     *
     * @param groupId
     */
    public void groupOnlineAndBroadcast(String groupId){
        groupOnline(groupId);
    }


    /**
     * @param user
     * @return 上线了所有群
     */
    public int userOnlineAll(User user) {
        IMap<String, ConcurrentHashMap<String, User>> activeGroupUser = getActiveGroupUser();
        user.setPassWord("");
        List<String> groupIdList = groupService.getGroupListByUserId(user.getUserId());
        //计数器
        int count = 0;
        for (String groupId : groupIdList) {
            ConcurrentHashMap<String, User> groupMap = activeGroupUser.get(groupId);
            groupMap.put(user.getUserId(), user);
            activeGroupUser.set(groupId,groupMap);
            count++;
        }
        return count;
    }

    /**
     * 上线单个群
     *
     * @param groupId
     * @param user
     * @return 上线线数量
     */
    public int userOnline(String groupId, User user) {
        IMap<String, ConcurrentHashMap<String, User>> activeGroupUser = getActiveGroupUser();
        user.setPassWord("");
        ConcurrentHashMap<String, User> groupMap = activeGroupUser.get(groupId);
        groupMap.put(user.getUserId(), user);
        activeGroupUser.set(groupId,groupMap);
        //返回上线数量
        return 1;
    }

    /**
     * @param userId
     * @return 下线所有群
     */
    public int userOfflineAll(String userId) {
        IMap<String, ConcurrentHashMap<String, User>> activeGroupUser = getActiveGroupUser();
        List<String> groupIdList = groupService.getGroupListByUserId(userId);
        //计数器
        int count = 0;
        for (String groupId : groupIdList) {
            ConcurrentHashMap<String, User> groupMap = activeGroupUser.get(groupId);
            groupMap.remove(userId);
            activeGroupUser.set(groupId,groupMap);
            count++;
        }
        return count;
    }

    /**
     * 下线单个
     *
     * @param groupId
     * @param user
     * @return 下线数量
     */
    public int userOffline(String groupId, User user) {
        IMap<String, ConcurrentHashMap<String, User>> activeGroupUser = getActiveGroupUser();
        ConcurrentHashMap<String, User> groupMap = activeGroupUser.get(groupId);
        groupMap.remove(user.getUserId());
        activeGroupUser.set(groupId,groupMap);
        //返回下线数量
        return 1;
    }

    /**
     * @return 群成员map
     */
    public ConcurrentHashMap<String, User> getGroupMembers(String groupId) {
        IMap<String, ConcurrentHashMap<String, User>> activeGroupUser = getActiveGroupUser();
        return activeGroupUser.get(groupId);
    }

    /**
     * 查询一个成员是否属于该群
     *
     * @param groupId
     * @param userId
     * @return
     */
    public boolean isGroupMember(String groupId, String userId) {
        ConcurrentHashMap<String, User> groupMembers = getGroupMembers(groupId);
        if (Objects.isNull(groupMembers)) {
            return false;
        }
        User user = groupMembers.get(userId);
        if (Objects.isNull(user)) {
            return false;
        }
        return true;
    }
}
