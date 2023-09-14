package cn.xk.chatBack.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * 重试队列用于消息的超时重发
 *
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-11
 */
public class RetryQueue {

    /**
     * 循环间隔
     */
    public static final int timeout = 3000;

    public static final int retryCount  = 3000;

    Map<Long, RetryMessage> map = new HashMap<>();

    //加一个定时任务注解
    public void timeLoop() {
        //获取当前时间戳
        long nowTime = System.currentTimeMillis();
        //遍历set
        for (RetryMessage retryMessage : map.values()) {
            if (nowTime - retryMessage.getTimeStamp() > timeout){
                //执行重发任务

            }else{
                retryMessage.setTimer(retryMessage.getTimer()+1);
            }
            if (retryMessage.getTimer() > retryCount){
                //发送失败
                //数据出队列
                map.remove(retryMessage);
            }
        }
    }

    public void ackMessage(long id){
        map.remove(id);
    }


}
