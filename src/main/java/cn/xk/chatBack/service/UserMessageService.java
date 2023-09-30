package cn.xk.chatBack.service;

import cn.xk.chatBack.model.message.Message;
import cn.xk.chatBack.model.R;
import cn.xk.chatBack.model.message.UserMessage;
import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.SocketIOClient;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-04
 */
public interface UserMessageService {
    int insertUserMessage(UserMessage userMessage);

    public boolean relayMessage(String targetServerHost, String targetId, String targetType, R<Object> message);

    public void sendMessage(String targetId, String targetType, R<Object> object);

    public boolean sendEvent(String name, String userId, Object o);

    public boolean relayEvent(String name, String userId, String targetServer, Object o);

    public void ackMessage(String messageType, Object message);

    public void change2WSProtocol(String targetServer);
}
