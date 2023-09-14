package cn.xk.chatBack.service.Impl;

import cn.xk.chatBack.mapper.UserMessageMapper;
import cn.xk.chatBack.model.constant.TargetType;
import cn.xk.chatBack.model.message.Message;
import cn.xk.chatBack.model.R;
import cn.xk.chatBack.model.message.UserMessage;
import cn.xk.chatBack.model.connect.UserSocketPool;
import cn.xk.chatBack.model.constant.RCode;
import cn.xk.chatBack.service.UserMessageService;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.corundumstudio.socketio.SocketIOClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-04
 */
@Slf4j
@Service
public class UserMessageServiceImpl implements UserMessageService {
    @Resource
    private UserMessageMapper userMessageMapper;

    @Resource
    private NacosServiceManager nacosServiceManager;
    @Resource
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Resource
    private UserSocketPool userSocketPool;

    @Resource
    private HazelcastInstance hazelcastInstance;

    @Value("${spring.application.name}")
    private String serverName;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final MediaType XML = MediaType.parse("application/xml; charset=utf-8");

    private OkHttpClient client = new OkHttpClient();

    @Override
    public int insertUserMessage(UserMessage userMessage) {
        return userMessageMapper.insert(userMessage);
    }

    @Override
    public boolean relayMessage(String targetServerHost, String targetId, String targetType, Object message) {
        //从nacos获取对应服务host
        Response response = null;
        try {
            //构建host
            NamingService namingService = nacosServiceManager.getNamingService(nacosDiscoveryProperties.getNacosProperties());
            Instance instance = namingService.selectOneHealthyInstance(targetServerHost);
            String ip = instance.getIp();
            int port = instance.getPort();
            String url = "http://" + ip + ":" + port + "/message/relayMessage";

            //构建请求体
            String jsonString = com.alibaba.fastjson2.JSON.toJSONString(message);
            RequestBody requestBody = RequestBody.create(JSON, jsonString);

            //构建请求
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .header("targetId", targetId)
                    .header("targetType", targetType)
                    .build();
            log.info("转发消息,目标主机{}",targetServerHost);
            response = client.newCall(request).execute();
            response.close();
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            IMap<Object, Object> userOnlineMap = hazelcastInstance.getMap("userOnlineMap");
            userOnlineMap.remove(targetId);
            if(!Objects.isNull(response)) {
                response.close();
            }
            return false;
        }
    }

    /**
     * 判断对方是否再本机或者其他服务器在线进行跨集群发送
     *
     * @param targetId
     * @return
     */
    @Override
    public boolean sendMessage(String targetId, String targetType, Object message) {
        //判断对方本服务器在线
        if (userSocketPool.isOnline(targetId)) {
            SocketIOClient client = userSocketPool.getSocket(targetId);

            if (TargetType.USER.equals(targetType)) {
                client.sendEvent("friendMessage", message);
            } else {
                client.sendEvent("groupMessage", message);
            }
            //todo 根据ack判断
            return true;
        } else {
            //不在线再分布式内存中判断是否在线在线则发送不在线则返回
            IMap<String, String> userOnlineMap = hazelcastInstance.getMap("userOnlineMap");
            //在线的服务器
            String targetServer = userOnlineMap.get(targetId);
            if (StringUtils.hasText(targetServer)&&!targetServer.equals(serverName)) {
                //转发消息
                return relayMessage(targetServer, targetId, targetType, message);
            } else {
                return false;
            }
        }
    }

    /**
     * 向用户发送事件
     *
     * @param name
     * @param userId
     * @param o
     */
    @Override
    public boolean sendEvent(String name, String userId, Object o) {
        //判断本地在线
        log.info("name:{},userId:{},online:{}",name,userId,userSocketPool.isOnline(userId));
        if (!userSocketPool.isOnline(userId)) {
            IMap<String, String> userOnlineMap = hazelcastInstance.getMap("userOnlineMap");
            String targetServer = userOnlineMap.get(userId);
            if (StringUtils.hasText(targetServer)&&!targetServer.equals(serverName)) {
                //消息转发
                return relayEvent(name, userId, targetServer, o);
            } else {
                //不在线
                return false;
            }
        }
        SocketIOClient socket = userSocketPool.getSocket(userId);
        socket.sendEvent(name, o);
        //todo ack
        return true;
    }
    @Override
    public boolean relayEvent(String name, String userId, String targetServer, Object o) {
        //构建host
        Response response = null;
        try {
            NamingService namingService = nacosServiceManager.getNamingService(nacosDiscoveryProperties.getNacosProperties());

            Instance instance = namingService.selectOneHealthyInstance(targetServer);
            String ip = instance.getIp();
            int port = instance.getPort();
            String url = "http://" + ip + ":" + port + "/message/relayEvent";

            //构建请求体
            String jsonString = com.alibaba.fastjson2.JSON.toJSONString(o);
            RequestBody requestBody = RequestBody.create(JSON, jsonString);

            //构建请求
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .header("name", name)
                    .header("userId", userId)
                    .build();
            log.info("转发事件:{},目标主机{}",name,targetServer);

            response = client.newCall(request).execute();
            response.close();

            //todo ack
            return true;

        } catch (Exception e) {
            if(!Objects.isNull(response)){
                response.close();
            }
            IMap<Object, Object> userOnlineMap = hazelcastInstance.getMap("userOnlineMap");
            userOnlineMap.remove(userId);
            log.error(e.getMessage());
            return false;
        }
    }
}
