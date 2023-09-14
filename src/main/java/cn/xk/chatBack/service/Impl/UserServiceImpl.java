package cn.xk.chatBack.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.xk.chatBack.mapper.UserMapper;
import cn.xk.chatBack.model.*;
import cn.xk.chatBack.model.message.UserMessage;
import cn.xk.chatBack.model.message.UserMsgList;
import cn.xk.chatBack.service.FriendMessageService;
import cn.xk.chatBack.service.GroupService;
import cn.xk.chatBack.service.UserService;
import cn.xk.chatBack.utils.RedisCache;
import cn.xk.chatBack.utils.exception.XkException;
import cn.xk.chatBack.utils.jwt.JwtUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author xk
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    AuthenticationManager authenticationManager;

    @Resource
    GroupService groupService;

    @Resource
    UserMapper userMapper;

    @Resource
    RedisCache redisCache;

    @Resource
    PasswordEncoder passwordEncoder;

    @Resource
    FriendMessageService friendMessageService;

    @Override
    public Map<String, Object> login(AuthUserRequestVo authUserRequestVo) {

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(authUserRequestVo.getUserName(), authUserRequestVo.getPassWord());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        if (Objects.isNull(authentication)) {
            throw new XkException(201, "登录失败");
        }
        //认证成功生成jwt, jwt存入Result中, 返回
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String id = loginUser.getUser().getUserId();
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        String json = JSON.toJSONString(map);
        String jwt = JwtUtil.createJWT(json);
        //userid作为key
        //把LoginUser对象存入redis

//        User userMap = loginUser.getUser();
        log.info("token: " + jwt);

        redisCache.setCacheObject("chatLogin:" + loginUser.getUser().getUserId(), loginUser);
        map = new HashMap<>();
        map.put("user", loginUser.getUser());
        map.put("token", jwt);
        return map;
    }

    @Override
    public int register(AuthUserRequestVo authUserRequestVo) {
        //todo 邮箱认证
        User user = new User();
        user.setUserName(authUserRequestVo.getUserName());
        user.setPassWord(passwordEncoder.encode(authUserRequestVo.getPassWord()));
        user.setCreateTime(System.currentTimeMillis());
        int result = userMapper.insert(user);
        if (result > 0) {
            //加入默认群
            int count = groupService.joinGroup(user.getUserId(), "lobby");
        }
        return result;
    }

    @Caching(evict = {
            @CacheEvict(value = "userByUserId", key = "#user.userId"),
    })
    @Override
    public int changeUserName(User user) {
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", user.getUserId());
        wrapper.set("user_name", user.getUserName());
        return userMapper.update(null, wrapper);
    }

    @Override
    public int changePassword(User user) {
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", user.getUserId());
        wrapper.set("password", user.getPassWord());
        return userMapper.update(null, wrapper);
    }

    /**
     * 获取好友消息列表
     *
     * @param userId
     * @return
     */
    @Override
    public List<UserMsgList> getUserChatByUserId(String userId) {
        //查好友列表
        List<UserMsgList> userMsgLists = new ArrayList<>();
        List<User> userList = userMapper.getUserList(userId);
        for (User user : userList) {
            //todo 这个30改成在配置里配置，含义为预加载多少数据
            List<UserMessage> userMessages = friendMessageService.selectMsgList(30, userId, user.getUserId());
            UserMsgList userMsgList = BeanUtil.copyProperties(user, UserMsgList.class);
            userMsgList.setMessages(userMessages);
            userMsgLists.add(userMsgList);
        }
        return userMsgLists;
    }

    @Cacheable("userByUserId")
    @Override
    public User selectUserByUserId(String userId) {
        User user = userMapper.selectById(userId);
        if (!Objects.isNull(user))
            user.setPassWord("");
        return user;
    }

    @Cacheable(value = "userRelationship",key = "#userId+#friendId")
    @Override
    public UserRelationship selectUserRelationship(String userId, String friendId) {
        return userMapper.selectUserRelationship(userId, friendId);

    }

    @Caching(evict = {
            @CacheEvict(value = "userByUserId", key = "#user.userId"),
    })
    @Override
    public int updateUser(User user) {
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_name", user.getUserName());
        return userMapper.update(user, wrapper);
    }

    @Caching(evict = {
            @CacheEvict(value = "userRelationship", key ="#userId+#friendId" ),
            @CacheEvict(value = "friendExist", key ="#userId+#friendId" ),
            @CacheEvict(value = "friendIdList", key ="#userId" ),
    })
    @Override
    public int addFriend(String userId, String friendId) {
        return userMapper.addFriend(userId, friendId);
    }

    @Cacheable("friendExist")
    @Override
    public int friendExist(String userId, String friendId) {
        return userMapper.friendExist(userId, friendId);
    }

    //todo 模糊查询
    @Override
    public List<User> getUsersByName(String userName) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.like("user_name", userName);
        return userMapper.selectList(wrapper);
    }

    @Caching(evict = {
            @CacheEvict(value = "userRelationship", key ="#userId+#friendId" ),
            @CacheEvict(value = "friendExist", key ="#userId+#friendId" ),
            @CacheEvict(value = "friendIdList", key ="#userId" ),
    })
    @Override
    public int exitFriend(String userId, String friendId) {
        return userMapper.exitFriend(userId, friendId);
    }

    @Cacheable("friendIdList")
    @Override
    public List<String> getFriendIdList(String userId) {
        return userMapper.getFriendIdList(userId);

    }
}
