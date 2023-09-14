package cn.xk.chatBack.mapper;

import cn.xk.chatBack.model.User;
import cn.xk.chatBack.model.UserRelationship;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author xk
 */
@Repository
public interface UserMapper extends BaseMapper<User> {
    List<User> getUserList(String userId);

    UserRelationship selectUserRelationship(String userId, String friendId);

    int addFriend(String userId, String friendId);

    int friendExist(String userId, String friendId);

    int exitFriend(String userId, String friendId);

    List<String> getFriendIdList(String userId);
}
