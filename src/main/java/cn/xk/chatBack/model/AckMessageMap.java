package cn.xk.chatBack.model;

import com.corundumstudio.socketio.AckMode;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.protocol.AckArgs;
import com.corundumstudio.socketio.store.HazelcastStore;
import com.hazelcast.cache.HazelcastCacheManager;

import java.util.concurrent.ConcurrentMap;

/**
 * ackMap
 *
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-21
 */
public class AckMessageMap {

    ConcurrentMap<String, AckRequest> ackMessageMap;
}
