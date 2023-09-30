package cn.xk.chatBack.model;

import com.corundumstudio.socketio.SocketIOClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxySocketIoClient implements SocketClient {

    private SocketIOClient client;

    @Override
    public void send(String eventName, Object object) {
        client.sendEvent(eventName,object);
    }

    @Override
    public void send(String eventName, Object ack, Object object) {
        //todo 待实现
        client.sendEvent(eventName,ack,object);
    }

    @Override
    public SocketClient close() {
        return this;
    }

    @Override
    public boolean isConnection(){
       return client.isChannelOpen();
    }
}
