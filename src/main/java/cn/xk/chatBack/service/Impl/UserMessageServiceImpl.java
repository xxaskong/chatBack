package cn.xk.chatBack.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.xk.chatBack.mapper.UserMessageMapper;
import cn.xk.chatBack.model.AckEntry;
import cn.xk.chatBack.model.R;
import cn.xk.chatBack.model.SocketClient;
import cn.xk.chatBack.model.constant.TargetType;
import cn.xk.chatBack.model.constant.TransportProtocol;
import cn.xk.chatBack.model.message.GroupMessage;
import cn.xk.chatBack.model.message.RelayMessageRequestVo;
import cn.xk.chatBack.model.message.UserMessage;
import cn.xk.chatBack.model.connect.UserSocketPool;
import cn.xk.chatBack.service.UserMessageService;
import cn.xk.chatBack.utils.GlobalTaskScheduler;
import cn.xk.chatBack.utils.SocketIoConnectionPool;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.SocketIOClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

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

    @Resource
    SocketIoConnectionPool socketIoConnectionPool;

    @Resource
    private AckEntry ackEntry;

    @Resource
    private GlobalTaskScheduler globalTaskScheduler;

    @Value("${spring.application.name}")
    private String serverName;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final MediaType XML = MediaType.parse("application/xml; charset=utf-8");

    private OkHttpClient httpClient = new OkHttpClient();

    private final ConcurrentMap<String, Integer> statisticMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Boolean> isTaskMap = new ConcurrentHashMap<>();



    @Override
    public int insertUserMessage(UserMessage userMessage) {
        return userMessageMapper.insert(userMessage);
    }

    @Override
    public boolean relayMessage(String targetServer, String targetId, String targetType, R<Object> message) {
        int count =0;
        if(statisticMap.get(targetServer) != null){
            count = statisticMap.get(targetServer);
        }
        Boolean isAdd = isTaskMap.get(targetServer);
        if(isAdd == null || (!isAdd.booleanValue()) ){
            //判断参数主动调用协议升级
            //往全局任务调度添加延时
            globalTaskScheduler.addTask(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                    IMap<String, String> transportProtocolMap = hazelcastInstance.getMap("transportProtocolMap");
                    int count = statisticMap.get(targetServer);
                    //清空计数
                    statisticMap.put(targetServer,0);
                    //100为设定速率阈值
                    if(count/10>100){
                        change2WSProtocol(targetServer);
                    }
                    else {
                        change2HTTPProtocol(targetServer);
                    }
                    globalTaskScheduler.addTask(this,10, TimeUnit.MINUTES);
                }
            },10, TimeUnit.MINUTES);
            isTaskMap.put(targetServer,true);
        }
        if(count>10){
            log.info("{}连接降级为http",targetServer);
            change2HTTPProtocol(targetServer);

        }
        statisticMap.put(targetServer,count+1);
        IMap<String, String> transportProtocolMap = hazelcastInstance.getMap("transportProtocolMap");
        String transportProtocol = transportProtocolMap.get(targetServer);
        log.info("当前协议为{}",transportProtocol);
        if (TransportProtocol.HTTP.equals(transportProtocol)){
            return reMessageByHttpClient(targetServer, targetId, targetType, message);
        }
        return relayMessageBySocketIo(targetServer, targetId, targetType, message);
    }
    @Override
    public void change2WSProtocol(String targetServer){
        IMap<String, String> transportProtocolMap = hazelcastInstance.getMap("transportProtocolMap");
        String transportProtocol = transportProtocolMap.get(targetServer);
        if (TransportProtocol.WS.equals(transportProtocol)) {
            return;
        }
        log.info("升级协议为ws:{}",serverName);
        transportProtocol = TransportProtocol.WS;
        transportProtocolMap.set(targetServer,TransportProtocol.WS);
        transportProtocolMap.set(serverName,TransportProtocol.WS);
    }

    public void change2HTTPProtocol(String targetServer){
        IMap<String, String> transportProtocolMap = hazelcastInstance.getMap("transportProtocolMap");
        String transportProtocol = transportProtocolMap.get(targetServer);
        if(TransportProtocol.HTTP.equals(transportProtocol)){
            return;
        }
        transportProtocol = TransportProtocol.HTTP;
        transportProtocolMap.set(targetServer,transportProtocol);
        socketIoConnectionPool.closeConnection(targetServer);
    }

    public boolean relayMessageBySocketIo(String targetServer, String targetId, String targetType, R<Object> message){
        RelayMessageRequestVo relayMessageRequestVo = new RelayMessageRequestVo();
        relayMessageRequestVo.setTargetId(targetId);
        relayMessageRequestVo.setTargetType(targetType);
        relayMessageRequestVo.setMessage(message);
        //获取目标主机连接
        SocketClient socketClient = socketIoConnectionPool.getConnection(targetServer);
        if(socketClient == null){
            log.info("获取{}连接失败采用http发送",targetServer);
            reMessageByHttpClient(targetServer,targetId,targetType,message);
            return true;
        }
        log.info("获取{}连接成功采用ws发送",targetServer);
        socketClient.send("/ws/relayMessage", relayMessageRequestVo);
        return true;
    }

    public boolean reMessageByHttpClient(String targetServerHost, String targetId, String targetType, R<Object> message){
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
            RelayMessageRequestVo relayMessageRequestVo = new RelayMessageRequestVo();
            relayMessageRequestVo.setTargetId(targetId);
            relayMessageRequestVo.setTargetType(targetType);
            relayMessageRequestVo.setMessage(message);
            String jsonString = com.alibaba.fastjson2.JSON.toJSONString(relayMessageRequestVo);
            RequestBody requestBody = RequestBody.create(JSON, jsonString);

            //构建请求
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .header("targetId", targetId)
                    .header("targetType", targetType)
                    .build();
            log.info("转发消息,目标主机{}", targetServerHost);
            response = httpClient.newCall(request).execute();
            response.close();
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            IMap<Object, Object> userOnlineMap = hazelcastInstance.getMap("userOnlineMap");
            userOnlineMap.remove(targetId);
            if (!Objects.isNull(response)) {
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
    public void sendMessage(String targetId, String targetType, R<Object> message) {
        //判断对方本服务器在线

        if (userSocketPool.isOnline(targetId)) {
            SocketIOClient targetClient = userSocketPool.getSocket(targetId);

            if (TargetType.USER.equals(targetType)) {
                //todo 超时重发

                targetClient.sendEvent("friendMessage", new AckCallback<String>(String.class) {
                    @Override
                    public void onSuccess(String localResult) {
                        log.info("client接收ack");
                        UserMessage userMessage = BeanUtil.copyProperties(message.getData(), UserMessage.class);
                        ackMessage("user", userMessage);
                    }
                }, new Object[]{message});

            } else {
                targetClient.sendEvent("groupMessage", message);
            }
            return;
        } else {
            //不在线到分布式内存中判断是否在线在线则发送不在线则返回
            IMap<String, String> userOnlineMap = hazelcastInstance.getMap("userOnlineMap");
            //在线的服务器
            String targetServer = userOnlineMap.get(targetId);
            if (StringUtils.hasText(targetServer) && !targetServer.equals(serverName)) {
                //转发消息
                relayMessage(targetServer, targetId, targetType, message);
                return;
            } else {
                if (TargetType.USER.equals(targetType)) {
                    UserMessage userMessage = BeanUtil.copyProperties(message.getData(), UserMessage.class);
                    ackMessage("user", userMessage);
                }
                return;
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
        log.info("name:{},userId:{},online:{}", name, userId, userSocketPool.isOnline(userId));
        if (!userSocketPool.isOnline(userId)) {
            IMap<String, String> userOnlineMap = hazelcastInstance.getMap("userOnlineMap");
            String targetServer = userOnlineMap.get(userId);
            if (StringUtils.hasText(targetServer) && !targetServer.equals(serverName)) {
                //消息转发
                return relayEvent(name, userId, targetServer, o);
            } else {
                //不在线
                return false;
            }
        }
        SocketIOClient socket = userSocketPool.getSocket(userId);
        socket.sendEvent(name, o);
        return true;
    }

    public void relayAckMessage(String targetServer, String messageType, Object message) {
        Response response = null;
        String jsonString = "";
        IMap<String, String> userOnlineMap = hazelcastInstance.getMap("userOnlineMap");
        try {
            NamingService namingService = nacosServiceManager.getNamingService(nacosDiscoveryProperties.getNacosProperties());
            Instance instance = namingService.selectOneHealthyInstance(targetServer);
            String ip = instance.getIp();
            int port = instance.getPort();
            String url = "http://" + ip + ":" + port + "/message/ackMessage";
            System.out.println(url);
            jsonString = com.alibaba.fastjson2.JSON.toJSONString(message);
            RequestBody requestBody = RequestBody.create(JSON, jsonString);
            //构建请求
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .header("messageType", messageType)
                    .build();
            log.info("ackMessage:{},目标主机{}",jsonString ,targetServer);
            response = httpClient.newCall(request).execute();
            response.close();
        } catch (Exception e) {
            e.printStackTrace();
            if (!Objects.isNull(response)) {
                response.close();
            }
            log.error("ackMessage失败:{},目标主机{}", jsonString, targetServer);
        }
    }

    @Override
    public void ackMessage(String messageType, Object message) {
        long messageId = 0;
        String sourceUserId;
        if (TargetType.USER.equals(messageType)) {
            UserMessage userMessage = BeanUtil.copyProperties(message, UserMessage.class);
            System.out.println(userMessage.getId());
            messageId = userMessage.getId();
            sourceUserId = userMessage.getUserId();
        } else {
            GroupMessage groupMessage = BeanUtil.copyProperties(message, GroupMessage.class);
            messageId = groupMessage.getId();
            sourceUserId = groupMessage.getUserId();
        }

        if (!userSocketPool.isOnline(sourceUserId)) {
            IMap<String, String> userOnlineMap = hazelcastInstance.getMap("userOnlineMap");
            String targetServer = userOnlineMap.get(sourceUserId);
            if (StringUtils.hasText(targetServer) && !targetServer.equals(serverName)) {
                //ack转发
                relayAckMessage(targetServer, messageType, message);
                return;
            } else {
                //不在线
                log.info("不在线:{},{}",serverName,targetServer);
                return;
            }
        }
        System.out.println(messageId);
        AckCallback<?> ackCallback = ackEntry.cancelCallback(messageId);
        if (!Objects.isNull(ackCallback))
            ackCallback.onSuccess(null);
        else{
            log.info("ackCallback为null");
        }

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
            log.info("转发事件:{},目标主机{}", name, targetServer);

            response = httpClient.newCall(request).execute();
            response.close();

            //todo ack
            return true;

        } catch (Exception e) {
            if (!Objects.isNull(response)) {
                response.close();
            }
            IMap<Object, Object> userOnlineMap = hazelcastInstance.getMap("userOnlineMap");
            userOnlineMap.remove(userId);
            log.error(e.getMessage());
            return false;
        }
    }

}
