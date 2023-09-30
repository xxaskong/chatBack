package cn.xk.chatBack.model;

import cn.xk.chatBack.model.connect.UserSocketPool;
import cn.xk.chatBack.model.constant.RCode;
import cn.xk.chatBack.model.constant.TargetType;
import cn.xk.chatBack.model.message.UserMessage;
import cn.xk.chatBack.service.UserMessageService;
import com.corundumstudio.socketio.SocketIOClient;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * 重试队列用于消息的超时重发
 *
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-11
 */
@Slf4j
public class RetryQueue {

    /**
     * 循环间隔
     */
    public static final int timeout = 3000;

    //重试次数
    public static final int retryCount  = 3;

    UserMessageService userMessageService;

    private final HashedWheelTimer executorService;

    ConcurrentMap<Integer, Timeout> timeoutMap;

    ConcurrentMap<Integer, Integer> timeoutFrequencyMap;

    public RetryQueue() {
        executorService = new HashedWheelTimer();
        timeoutMap = new ConcurrentHashMap<>();
        timeoutFrequencyMap = new ConcurrentHashMap<>();
    }

    public void registerMessage(int messageId){
        timeoutFrequencyMap.put(messageId,0);
    }

    public void proxyMessage(int messageId, TimerTask timerTask, Object lock){
        synchronized (lock) {
            Integer timeoutFrequency = timeoutFrequencyMap.get(messageId);
            if (!Objects.isNull(timeoutFrequency) && timeoutFrequency < retryCount) {
                log.info("添加超时xxask{}",messageId);
                Timeout timeout = executorService.newTimeout(timerTask, 3, TimeUnit.SECONDS);
                timeoutMap.put(messageId, timeout);
                timeoutFrequencyMap.put(messageId, timeoutFrequency + 1);
            }
        }
    }

    public void cancelMessage(Integer messageId, Object lock){
        synchronized (lock) {
            timeoutFrequencyMap.put(messageId, retryCount + 1);
            Timeout timeout = timeoutMap.remove(messageId);
            if (timeout!= null) {
                timeout.cancel();
            }
        }
    }


}
