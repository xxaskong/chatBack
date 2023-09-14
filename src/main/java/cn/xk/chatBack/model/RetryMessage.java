package cn.xk.chatBack.model;

import lombok.Data;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-11
 */
@Data
public class RetryMessage {

    long id;

    int timer;

    long timeStamp;

    Object message;

}
