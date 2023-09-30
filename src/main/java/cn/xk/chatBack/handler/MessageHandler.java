package cn.xk.chatBack.handler;

import cn.hutool.core.bean.BeanUtil;
import cn.xk.chatBack.model.*;
import cn.xk.chatBack.model.connect.ActiveGroupUser;
import cn.xk.chatBack.model.connect.UserSocketPool;
import cn.xk.chatBack.model.constant.MessageType;
import cn.xk.chatBack.model.constant.RCode;
import cn.xk.chatBack.model.constant.TargetType;
import cn.xk.chatBack.model.message.*;
import cn.xk.chatBack.service.GroupMessageService;
import cn.xk.chatBack.service.GroupService;
import cn.xk.chatBack.service.UserMessageService;
import cn.xk.chatBack.service.UserService;
import cn.xk.chatBack.utils.AliOSSUtils;
import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.hazelcast.core.HazelcastInstance;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-08
 */
@Slf4j
@Component
public class MessageHandler {

    @Autowired
    private SocketIOServer socketIoServer;

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    //群里在线的群成员
    @Autowired
    private ActiveGroupUser activeGroupUser;

    @Autowired
    public UserSocketPool userSocketPool;

    @Autowired
    private UserMessageService userMessageService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private GroupMessageService groupMessageService;

    @Autowired
    private AliOSSUtils aliOSSUtils;

    @Autowired
    AckEntry ackEntry;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private RetryQueue retryQueue;

    @OnEvent("abc")
    public void test(SocketIOClient client, String s){
        log.info("test:{}",s);
    }





    @OnEvent("friendMessage")
    public void friendMessage(SocketIOClient client, UserMessageVo userMessageVo, final AckRequest ackRequest) throws IOException {
        long nowTime = System.currentTimeMillis();
        userMessageVo.setTime(nowTime);
        User user = client.get("user");
        R<Object> result = new R<>();
        //判断是不是本用户发送的消息
        log.info("user：{}", user);
        if (!user.getUserId().equals(userMessageVo.getUserId())) {
            result.setCode(RCode.FAIL);
            result.setMsg("发送消息错误,请重试");
            result.setData(userMessageVo);
            client.sendEvent("friendMessage", result);
            return;
        }
        //获取好友的socket

        //判断有没有好友关系和好友是否在线
        //todo 好友关系和好友在线用client缓存和redis缓存实现，mysql为兜底策略
        List<String> friendIdList = client.get("friendIdList");
        //判断有没有好友关系,先判断缓存若缓存不存在则继续查数据库两个都不存在则确定不存在好友关系
        if (!friendIdList.contains(userMessageVo.getFriendId()) &&
                (userService.friendExist(userMessageVo.getUserId(), userMessageVo.getFriendId()) > 0)) {
            result.setCode(RCode.FAIL);
            result.setMsg("对方不是你的好友,请添加好友后");
            result.setData(userMessageVo);
            client.sendEvent("friendMessage", result);
            return;
        }
        UserMessage userMessage = BeanUtil.copyProperties(userMessageVo, UserMessage.class);
        //判断消息类型
        if (MessageType.IMG.equals(userMessageVo.getMessageType())) {
            //图片类型
            ByteBuffer imgContent = userMessageVo.getImgContent();
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String fileName = uuid + "$" + userMessageVo.getWidth() + "$" + userMessageVo.getHeight();
            String imgUrl = aliOSSUtils.upload(fileName, imgContent);
            //存入图片访问路径
            userMessage.setContent(imgUrl);
        }




        //消息存库
        int count = userMessageService.insertUserMessage(userMessage);

        if (count == 0) {
            result.setCode(RCode.FAIL);
            result.setMsg("发送失败请重试");
            result.setData(userMessageVo);
            client.sendEvent("friendMessage", result);
            return;
        }
        result.setCode(RCode.OK);
        result.setMsg("");
        result.setData(userMessage);


        ackEntry.registerAckCallback(userMessage.getId(), new AckCallback<String>(String.class) {
            @Override
            public void onSuccess(String localResult) {
                //给自己发一份表示发送成功
                client.sendEvent("friendMessage", result);
                log.info("ackToClient:{}",userMessage.getContent());
                retryQueue.cancelMessage(userMessage.getId(),userMessage);
            }
        });

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                log.info("添加超时重传{}",userMessage.getId());
                userMessageService.sendMessage(userMessage.getFriendId(), TargetType.USER, result);
                retryQueue.proxyMessage(userMessage.getId(), this,userMessage);
            }
        };
        userMessageService.sendMessage(userMessage.getFriendId(), TargetType.USER, result);
        retryQueue.registerMessage(userMessage.getId());
        retryQueue.proxyMessage(userMessage.getId(), timerTask , userMessage);
    }

    @OnEvent("groupMessage")
    public void groupMessage(SocketIOClient client, GroupMessageVo groupMessageVo) throws IOException {
        long nowTime = System.currentTimeMillis();
        groupMessageVo.setTime(nowTime);
        R<Object> result = new R<>();
        //判断用户是否和userId一致
        User user = client.get("user");
        if (Objects.isNull(user) || !groupMessageVo.getUserId().equals(user.getUserId())) {
            result.setMsg(Objects.isNull(user) ? "获取用户失败" : "本机用户和发送所携带用户id不一致");
            result.setCode(RCode.FAIL);
            client.sendEvent("groupMessage", result);
            return;
        }
        //判断群存在不存在 和改用户属不属于该群
        ChatGroup group = groupService.selectGroupByGroupId(groupMessageVo.getGroupId());
        if (Objects.isNull(group)) {
            result.setCode(RCode.FAIL);
            result.setMsg("群不存在");
            client.sendEvent("groupMessage", result);
            return;
        }
        if (!activeGroupUser.isGroupMember(groupMessageVo.getGroupId(), groupMessageVo.getUserId())) {
            result.setCode(RCode.FAIL);
            result.setMsg("不属于该群");
            client.sendEvent("groupMessage", result);
            return;
        }
        //判断消息和将vo类转成普通类
        GroupMessage groupMessage = BeanUtil.copyProperties(groupMessageVo, GroupMessage.class);
        //判断消息类型 图片形则存库
        if (MessageType.IMG.equals(groupMessageVo.getMessageType())) {
            //todo 可靠性保证
            ByteBuffer imgContent = groupMessageVo.getImgContent();
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String fileName = uuid + "$" + groupMessageVo.getWidth() + "$" + groupMessageVo.getHeight();
            String imgUrl = aliOSSUtils.upload(fileName, imgContent);
            //存入图片访问路径
            groupMessage.setContent(imgUrl);
        }
        //消息存库
        int count = groupMessageService.insertGroupMessage(groupMessage);
        //消息通过集群间转发
        if (count > 0) {
            //存库成功在线发送
            result.setCode(RCode.OK);
            result.setMsg("");
            result.setData(groupMessage);
            groupMessageService.sendGroupMessage(groupMessage.getGroupId(), result);
            //todo 消息确认
        } else {
            result.setCode(RCode.FAIL);
            result.setMsg("消息发送失败请重试");
            client.sendEvent("groupMessage", result);
        }
    }


}
