package cn.xk.chatBack.service;

import cn.xk.chatBack.model.AuthUserRequestVo;
import cn.xk.chatBack.model.User;
import cn.xk.chatBack.model.message.UserMsgList;
import cn.xk.chatBack.model.UserRelationship;

import java.util.List;
import java.util.Map;

public interface UserService {

    Map<String, Object> login(AuthUserRequestVo authUserRequestVo);

    int register(AuthUserRequestVo authUserRequestVo);

    int changeUserName(User user);

    int changePassword(User user);

    List<UserMsgList> getUserChatByUserId(String userId);

    User selectUserByUserId(String userId);

    UserRelationship selectUserRelationship(String userId, String friendId);

    int updateUser(User user);

    int addFriend(String userId, String friendId);

    int friendExist(String userId, String friendId);

    List<User> getUsersByName(String userName);

    int exitFriend(String userId, String friendId);

    List<String> getFriendIdList(String userId);

}
