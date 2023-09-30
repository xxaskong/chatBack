package cn.xk.chatBack.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-27
 */
public interface SocketClient {

    void send(String eventName,Object object);

    void send(String eventName,Object ack,Object object);

    boolean isConnection();

    SocketClient close();
}
