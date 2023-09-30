package cn.xk.chatBack.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.xk.chatBack.controller.MessageController;
import cn.xk.chatBack.model.ProxySocketClient;
import cn.xk.chatBack.model.ProxySocketIoClient;
import cn.xk.chatBack.model.SocketClient;
import cn.xk.chatBack.model.message.RelayMessageRequestVo;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.fastjson2.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-27
 */
@Slf4j
@Component
public class SocketIoConnectionPool {

    @Resource
    private NacosServiceManager nacosServiceManager;

    @Resource
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Resource
    private MessageController messageController;

    ConcurrentMap<UUID,String> sessionMap;

    ConcurrentMap<String, SocketClient> socketPool;


    public SocketIoConnectionPool(){
        this.sessionMap = new ConcurrentHashMap<>();
        this.socketPool = new ConcurrentHashMap<>();

    }

    public void addMapping(UUID sessionId,String serverName){
        sessionMap.put(sessionId,serverName);
    }

    public String getMapping(UUID sessionId){
        return sessionMap.get(sessionId);
    }

    public void delMapping(UUID sessionId){
        sessionMap.remove(sessionId);
    }

    public void putConnection(String serverName,SocketClient socketClient){
        SocketClient insertConnection = socketPool.put(serverName, socketClient);
    }

    public SocketClient getConnection(String serverName){
        SocketClient socketClient = socketPool.get(serverName);
        if(socketClient == null ||!socketClient.isConnection()){
            createConnect(serverName);
            return socketPool.get(serverName);
        }
        return socketClient;
    }

    public void addListen(Socket socket,String eventName, Emitter.Listener listener){
        socket.on(eventName,listener);
    }

    public  void createConnect(String serverName){
        if (!Objects.isNull(getConnection(serverName))){
            return;
        }
        //获取服务的ip和端口
        //通过端口偏移量获得socketIo的连接地址
        Instance instance;
        try{
            NamingService namingService = nacosServiceManager.getNamingService(nacosDiscoveryProperties.getNacosProperties());
            instance = namingService.selectOneHealthyInstance(serverName);
        }
        catch (Exception e){
            //获取实例地址失败
            log.info("获取实例地址失败:{}",serverName);
            log.error(e.getMessage());
            return;
        }
        String ip = instance.getIp();
        int port = instance.getPort()+991;
        // 服务端socket.io连接通信地址
        String url = "http://"+ip+":"+port+"/?connectType=server"+"&targetServer="+serverName;
        try {
            IO.Options options = new IO.Options();
            options.transports = new String[]{"websocket","xhr-polling","jsonp-polling"};
            // 失败重试次数
            options.reconnectionAttempts = 3;
            // 失败重连的时间间隔
            options.reconnectionDelay = 1000;
            // 连接超时时间(ms)
            options.timeout = 500;
            //final Socket socket = IO.socket(url + "?userId=2", options);
            Socket socket = IO.socket(url, options);
            socket.connect();
            ProxySocketClient proxySocketClient = new ProxySocketClient(socket);
            addListen(socket, "/ws/relayMessage", new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    RelayMessageRequestVo relayMessageRequestVo = BeanUtil.copyProperties(objects, RelayMessageRequestVo.class);
                    messageController.wsRelayMessage(relayMessageRequestVo);
                }
            });
            socketPool.put(serverName,proxySocketClient);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeConnection(String serverName){
        SocketClient socket = socketPool.remove(serverName);
        if (Objects.isNull(socket)){
            socket.close();
        }
    }
}
