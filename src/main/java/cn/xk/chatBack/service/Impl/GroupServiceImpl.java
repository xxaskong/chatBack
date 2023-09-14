package cn.xk.chatBack.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.xk.chatBack.mapper.GroupMapper;
import cn.xk.chatBack.mapper.GroupMessageMapper;
import cn.xk.chatBack.model.*;
import cn.xk.chatBack.model.connect.ActiveGroupUser;
import cn.xk.chatBack.model.message.ChatGroupMsgList;
import cn.xk.chatBack.model.message.GroupMessage;
import cn.xk.chatBack.model.message.MessagePageRequestVo;
import cn.xk.chatBack.service.GroupService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-08-19
 */
@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    @Resource
    GroupMapper groupMapper;

    @Resource
    GroupMessageMapper groupMessageMapper;

    @Resource
    ActiveGroupUser activeGroupUser;

    //todo 模糊匹配
    @Override
    public List<ChatGroup> getGroupsByName(String groupName) {
        QueryWrapper<ChatGroup> wrapper = new QueryWrapper<>();
        wrapper.like("group_name",groupName);
        return groupMapper.selectList(wrapper);
    }

    //todo message类需要细调
    @Override
    public Page<GroupMessage> selectPages(MessagePageRequestVo messagePageRequestVo) {
        Page<GroupMessage> messagePage = new Page<>(messagePageRequestVo.getCurrent(), messagePageRequestVo.getPageSize());
        QueryWrapper<GroupMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id",messagePageRequestVo.getGroupId());
        wrapper.orderByDesc("time");
        messagePage = groupMessageMapper.selectPage(messagePage, wrapper);
        if(messagePage.getTotal() != 0){
            messagePage.getRecords().sort(new Comparator<GroupMessage>() {
                @Override
                public int compare(GroupMessage u1, GroupMessage u2) {
                    return u1.getId()-u2.getId();
                }
            });
        }
        return messagePage;
    }

    /**
     * 根据群名获取所有群成员
     *
     * @param groupId
     * @return
     */
    @Cacheable("usersInGroup")
    @Override
    public List<User> getUsersInGroup(String groupId) {
        return groupMapper.getUsersInGroup(groupId);
    }

    @Override
    public List<ChatGroupMsgList> getGroupChatByUserId(String userId) {
        List<ChatGroup> chatGroupList = groupMapper.getGroupChatByUserId(userId);
        List<ChatGroupMsgList> groupMsgLists = new ArrayList<>();
        QueryWrapper<GroupMessage> wrapper = new QueryWrapper<>();

        wrapper.last("limit 30");
        wrapper.orderByDesc("id");
        for (int i = 0; i < chatGroupList.size(); i++) {
            //拿n条记录
            ChatGroup chatGroup = chatGroupList.get(i);
            wrapper.eq("group_id", chatGroup.getGroupId());

            List<GroupMessage> groupMessages = groupMessageMapper.selectList(wrapper);
            //正序
            groupMessages.sort(new Comparator<GroupMessage>() {
                @Override
                public int compare(GroupMessage u1, GroupMessage u2) {
                    return u1.getId()-u2.getId();
                }
            });
            ChatGroupMsgList groupMsgList = BeanUtil.copyProperties(chatGroup, ChatGroupMsgList.class);
            groupMsgList.setMessages(groupMessages);
            groupMsgLists.add(groupMsgList);
        }
        return groupMsgLists;
    }

    /**
     * 通过id查群
     *
     * @param id
     * @return
     */
    @Cacheable("groupByGroupId")
    @Override
    public ChatGroup selectGroupByGroupId(String id) {
        return groupMapper.selectById(id);
    }

    /**
     * 获取所有群的唯一id列表
     *
     * @return
     */
    @Cacheable("allGroupId")
    @Override
    public List<String> selectAllGroupId() {
        return groupMapper.selectAllGroupId();
    }

    /**
     * 获取用户所在的群列表
     *
     * @param userId
     * @return
     */
    @Cacheable("groupListByUserId")
    @Override
    public List<String> getGroupListByUserId(String userId) {
        return groupMapper.getGroupListByUserId(userId);
    }




    //activeGroupUser
    @Override
    public RWS<?> selectActiveGroupUser(String userId){
        //返回所有与该userId有关的群组并且返回对应群里所在线的人数
        ConcurrentMap<String, User> groupMemberss = activeGroupUser.getGroupMembers("lobby");
        log.info("方法里人数before:{}", groupMemberss.keySet().size());
        ConcurrentMap<String, ConcurrentHashMap<String, User>> activeGroup = activeGroupUser.getActiveGroupUser();
        //user所属群聊列表
        List<String> groupList = getGroupListByUserId(userId);
        ConcurrentMap<String, ConcurrentHashMap<String, User>> resultMap =  new ConcurrentHashMap<>();
        //todu 这个tmdresultmap有bug！！！！！！！！！！！！！！！
        for (String groupId : groupList) {
            ConcurrentHashMap<String, User> userMap = activeGroup.get(groupId);
            if (userMap.size()>0){
                resultMap.put(groupId,userMap);
            }
        }
//        for (Map.Entry<String, ConcurrentHashMap<String, User>> entry : activeGroup.entrySet()) {
//            String groupId = entry.getKey();
//            ConcurrentHashMap<String, User> userMap = entry.getValue();
//            //广播给user在的群user下线了所以要查出一个user所属group列表遍历改列表
//            if (userMap.containsKey(userId)) {
//                resultMap.put(groupId,userMap);
//            }
//        }
        ConcurrentMap<String, User> groupMembers = activeGroupUser.getGroupMembers("lobby");
        log.info("方法里人数after:{}", groupMembers.keySet().size());
        RWS<ConcurrentMap<String, ConcurrentHashMap<String, User>>> ok = RWS.ok(resultMap);
        log.info("xxas:{}",resultMap.get("lobby").size());
        ok.setMsg("activeGroupUser");
        return ok;
    }

    /**
     * 新增群
     *
     * @param group
     * @return
     */
    @CacheEvict(value = "allGroupId",allEntries = true)
    @Override
    public int insertGroup(ChatGroup group) {
        return groupMapper.insert(group);
    }

    /**
     * 通过群名查找群
     *
     * @param groupName
     * @return
     */
    @Cacheable("groupByGroupName")
    @Override
    public ChatGroup selectGroupByGroupName(String groupName) {
        QueryWrapper<ChatGroup> wrapper = new QueryWrapper<>();
        wrapper.eq("group_name",groupName);
        return groupMapper.selectOne(wrapper);
    }

    @Caching(evict = {
            @CacheEvict(value = "usersInGroup", key = "#groupId"),
            @CacheEvict(value = "groupListByUserId", key = "#userId")
    })
    @Override
    public int joinGroup(String userId, String groupId) {
        return groupMapper.joinGroup(userId, groupId);
    }

    @Caching(evict = {
            @CacheEvict(value = "usersInGroup", key = "#groupId"),
            @CacheEvict(value = "groupListByUserId", key = "#userId")
    })
    @Override
    public int exitGroup(String userId, String groupId) {
        return groupMapper.exitGroup(userId, groupId);
    }


}
