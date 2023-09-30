package cn.xk.chatBack.model;

import com.alibaba.fastjson2.JSON;
import io.socket.client.Socket;
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
public class ProxySocketClient implements SocketClient {

    private Socket client;


    @Override
    public void send(String eventName, Object object) {
        String jsonString = JSON.toJSONString(object);
        client.emit(eventName,jsonString);
    }

    @Override
    public void send(String eventName, Object ack, Object object) {
        //todo 待实现
        client.emit(eventName,object,ack);
    }

    @Override
    public SocketClient close() {
        client.close();
        return this;
    }

    @Override
    public boolean isConnection(){
        return client.connected();
    }

}
