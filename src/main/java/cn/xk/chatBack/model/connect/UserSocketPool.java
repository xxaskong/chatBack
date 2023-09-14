package cn.xk.chatBack.model.connect;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: xk
 * @Description: 用户连接池
 * @CreateDate: Created in 2023-09-04
 */
public class UserSocketPool {

    private ConcurrentMap<String, SocketIOClient> socketIOClientMap;

    private ConcurrentMap<String, String> sessionIdMap;

    //初始化连接池
    @PostConstruct
    public void init() {
        this.sessionIdMap = new ConcurrentHashMap<>();
        this.socketIOClientMap = new ConcurrentHashMap<>();
    }

    public String getUserId(String sessionId){
        return sessionIdMap.get(sessionId);
    }

    public void addSession(String sessionId, String userId){
        sessionIdMap.put(sessionId,userId);
    }

    public void removeSession(String sessionId){
        sessionIdMap.remove(sessionId);
    }


    /**
     * 往连接池添加socket对象key为用户id
     * key存在的话返回key插入前的值插入当前值
     * key不存在返回null
     *
     * @param userId
     * @param socketIOClient
     * @return
     */
    public SocketIOClient addSocket(String userId, SocketIOClient socketIOClient) {
        SocketIOClient client = socketIOClientMap.put(userId, socketIOClient);
        return client;
    }

    /**
     * 删除连接池中的连接
     *
     * @param userId
     * @return
     */
    public SocketIOClient delSocket(String userId) {
        SocketIOClient socketIOClient = socketIOClientMap.remove(userId);
        return socketIOClient;
    }

    /**
     * 获取连接
     *
     * @param userId
     * @return
     */
    public SocketIOClient getSocket(String userId) {
        //todo 从redis获取
        //失败时从redis中获取如果两边都获取失败则证明不在线
        return socketIOClientMap.get(userId);
    }

    /**
     * 判断用户是否在线
     *
     * @param userId
     * @return
     */
    public boolean isOnline(String userId) {
        SocketIOClient socketIOClient = socketIOClientMap.get(userId);
        if (Objects.isNull(socketIOClient)) {
            return false;
        }
        return socketIOClient.isChannelOpen();
    }

}
