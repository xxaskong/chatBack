package cn.xk.chatBack.service;

import cn.xk.chatBack.model.*;
import cn.xk.chatBack.model.message.ChatGroupMsgList;
import cn.xk.chatBack.model.message.GroupMessage;
import cn.xk.chatBack.model.message.MessagePageRequestVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-08-19
 */
public interface GroupService {
    List<ChatGroup> getGroupsByName(String groupName);

    Page<GroupMessage> selectPages(MessagePageRequestVo messagePageRequestVo);

    List<User> getUsersInGroup(String groupId);

    List<ChatGroupMsgList> getGroupChatByUserId(String userId);

    ChatGroup selectGroupByGroupId(String groupId);

    List<String> selectAllGroupId();

    List<String> getGroupListByUserId(String userId);

    public RWS<?> selectActiveGroupUser(String userId);

    int insertGroup(ChatGroup group);

    ChatGroup selectGroupByGroupName(String groupName);

    int joinGroup(String userId, String groupId);

    int exitGroup(String userId, String groupId);
}
