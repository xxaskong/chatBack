package cn.xk.chatBack.service.Impl;

import cn.xk.chatBack.mapper.UserMessageMapper;
import cn.xk.chatBack.model.message.MessagePageRequestVo;
import cn.xk.chatBack.model.message.UserMessage;
import cn.xk.chatBack.model.connect.UserSocketPool;
import cn.xk.chatBack.service.FriendMessageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-08-19
 */
@Slf4j
@Service
public class FriendMessageServiceImpl implements FriendMessageService {

    @Resource
    private UserMessageMapper userMessageMapper;

    @Resource
    private UserSocketPool userSocketPool;

    @Override
    public List<UserMessage> selectMsgList(int num, String userId, String friendId) {
        QueryWrapper<UserMessage> wrapper = new QueryWrapper<>();
        wrapper.and(
                qw -> qw.eq("user_id", userId)
                        .eq("friend_id", friendId)
        ).or(
                qw -> qw.eq("user_id", friendId)
                        .eq("friend_id", userId)
        );
        wrapper.orderByDesc("time");
        List<UserMessage> userMessages = userMessageMapper.selectList(wrapper);

        if (!userMessages.isEmpty()) {
            userMessages.sort(new Comparator<UserMessage>() {
                @Override
                public int compare(UserMessage u1, UserMessage u2) {
                    return u1.getId() - u2.getId();
                }
            });
        }
        return userMessages;
    }
    @Override
    public Page<UserMessage> selectPages(MessagePageRequestVo messagePageRequestVo) {
        Page<UserMessage> messagePage = new Page<>(messagePageRequestVo.getCurrent(), messagePageRequestVo.getPageSize());

        QueryWrapper<UserMessage> wrapper = new QueryWrapper<>();
        wrapper.and(
                qw -> qw.eq("user_id", messagePageRequestVo.getUserId())
                        .eq("friend_id", messagePageRequestVo.getFriendId())
        ).or(
                qw -> qw.eq("user_id", messagePageRequestVo.getFriendId())
                        .eq("friend_id", messagePageRequestVo.getUserId())
        );
        wrapper.orderByDesc("time");
        messagePage = userMessageMapper.selectPage(messagePage, wrapper);

        if(messagePage.getTotal() != 0){
            messagePage.getRecords().sort(new Comparator<UserMessage>() {
                @Override
                public int compare(UserMessage u1, UserMessage u2) {
                    return u1.getId()-u2.getId();
                }
            });

        }
        return messagePage;
    }
}

