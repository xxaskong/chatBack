package cn.xk.chatBack.model;

import lombok.Data;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-11
 */
@Data
public class RetryMessage implements Delayed {

    long id;

    int timer;

    /**
     * 到期时间
     */
    long timeStamp;

    Object message;

    String messageType;

    @Override
    public long getDelay(TimeUnit unit) {
        return timeStamp -System.currentTimeMillis();
    }

    public void incrementTimer(){
        this.timer = timer+1;
    }

    @Override
    public int compareTo(Delayed o) {
        return (int) (getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }
}
