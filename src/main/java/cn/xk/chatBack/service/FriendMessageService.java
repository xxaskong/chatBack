package cn.xk.chatBack.service;

import cn.xk.chatBack.model.message.MessagePageRequestVo;
import cn.xk.chatBack.model.message.UserMessage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;


/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-08-19
 */
public interface FriendMessageService {

    Page<UserMessage> selectPages(MessagePageRequestVo messagePageRequestVo);

    List<UserMessage> selectMsgList(int num, String userId, String friendId);
}
