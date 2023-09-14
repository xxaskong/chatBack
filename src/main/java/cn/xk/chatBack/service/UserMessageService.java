package cn.xk.chatBack.service;

import cn.xk.chatBack.model.message.Message;
import cn.xk.chatBack.model.R;
import cn.xk.chatBack.model.message.UserMessage;
import com.corundumstudio.socketio.AckCallback;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-04
 */
public interface UserMessageService {
    int insertUserMessage(UserMessage userMessage);

    public boolean relayMessage(String targetServerHost, String targetId, String targetType, Object message);

    public boolean sendMessage(String targetId, String targetType, Object object);

    public boolean sendEvent(String name, String userId, Object o);

    public boolean relayEvent(String name, String userId, String targetServer, Object o);
}
