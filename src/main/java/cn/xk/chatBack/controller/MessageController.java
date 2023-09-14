package cn.xk.chatBack.controller;

import cn.xk.chatBack.handler.MessageHandler;
import cn.xk.chatBack.model.*;
import cn.xk.chatBack.model.connect.ActiveGroupUser;
import cn.xk.chatBack.model.connect.UserSocketPool;
import cn.xk.chatBack.model.message.Message;
import cn.xk.chatBack.service.UserMessageService;
import com.corundumstudio.socketio.SocketIOClient;
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
    public R<?> message(HttpServletRequest request, @RequestBody Object message) {
        String targetId = request.getHeader("targetId");
        String targetType = request.getHeader("targetType");
        userMessageService.sendMessage(targetId, targetType, message);
        return R.ok();
    }

    @PostMapping("/relayEvent")
    public R<?> event(HttpServletRequest request, @RequestBody Object o) {
        String name = request.getHeader("name");
        String userId = request.getHeader("userId");
        boolean result = userMessageService.sendEvent(name, userId, o);
        return result ? R.ok() : R.fail();
    }
}
