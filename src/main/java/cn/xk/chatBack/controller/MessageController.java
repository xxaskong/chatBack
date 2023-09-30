package cn.xk.chatBack.controller;

import cn.xk.chatBack.handler.MessageHandler;
import cn.xk.chatBack.model.*;
import cn.xk.chatBack.model.connect.ActiveGroupUser;
import cn.xk.chatBack.model.connect.UserSocketPool;
import cn.xk.chatBack.model.message.Message;
import cn.xk.chatBack.model.message.RelayMessageRequestVo;
import cn.xk.chatBack.service.UserMessageService;
import com.alibaba.fastjson2.JSON;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: xk
 * @Description: 接收其他服务端转发过来的消息
 * @CreateDate: Created in 2023-09-08
 */
@CrossOrigin
@Slf4j
@RestController
@RequestMapping("/message")
public class MessageController {

    @Resource
    MessageHandler messageHandler;

    @Resource
    public UserSocketPool userSocketPool;

    @Resource
    ActiveGroupUser activeGroupUser;

    @Resource
    private HazelcastInstance hazelcastInstance;

    @Resource
    private UserMessageService userMessageService;

    /**
     * 接收别的集群转发过来的消息
     *
     * @param message
     * @return
     */
    @PostMapping("/relayMessage")
    public R<?> message(@RequestBody RelayMessageRequestVo relayMessageRequestVo) {
        String targetId = relayMessageRequestVo.getTargetId();
        String targetType = relayMessageRequestVo.getTargetType();
        userMessageService.sendMessage(targetId, targetType, relayMessageRequestVo.getMessage());
        return R.ok();
    }

    @OnEvent("/ws/relayMessage")
    public void wsRelayMessage(RelayMessageRequestVo relayMessageRequestVo){
        String targetId = relayMessageRequestVo.getTargetId();
        String targetType = relayMessageRequestVo.getTargetType();
        userMessageService.sendMessage(targetId, targetType, relayMessageRequestVo.getMessage());
    }



    @PostMapping("/relayEvent")
    public R<?> event(HttpServletRequest request, @RequestBody Object o) {
        String name = request.getHeader("name");
        String userId = request.getHeader("userId");
        boolean result = userMessageService.sendEvent(name, userId, o);
        return result ? R.ok() : R.fail();
    }

    @PostMapping("/ackMessage")
    public R<?> ack(HttpServletRequest request, @RequestBody Object message){
        log.info("接收到转发过来的ack");
        log.info("{}", com.alibaba.fastjson2.JSON.toJSONString(message));
        String messageType = request.getHeader("messageType");
        long btime = System.currentTimeMillis();
        userMessageService.ackMessage(messageType, message);
        System.out.println(System.currentTimeMillis()-btime);
        return R.ok();
    }
}
