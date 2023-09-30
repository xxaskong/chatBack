package cn.xk.chatBack.utils;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-30
 */
@Component
public class GlobalTaskScheduler {

    private final HashedWheelTimer executorService;

    public GlobalTaskScheduler() {
        executorService = new HashedWheelTimer();
    }

    public void addTask(TimerTask task, long delay, TimeUnit unit){
        Timeout timeout = executorService.newTimeout(task, 3, TimeUnit.SECONDS);
    }
}
