package cn.xk.chatBack.mapper;

import cn.xk.chatBack.model.ChatGroup;
import cn.xk.chatBack.model.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-08-19
 */
@Repository
public interface GroupMapper extends BaseMapper<ChatGroup> {

    List<User> getUsersInGroup(@Param("groupId") String groupId);

    List<ChatGroup> getGroupChatByUserId(String userId);

    List<String> selectAllGroupId();

    List<String> getGroupListByUserId(String userId);

    int joinGroup(String userId, String groupId);

    int exitGroup(String userId, String groupId);
}
