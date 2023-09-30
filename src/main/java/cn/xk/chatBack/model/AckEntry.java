package cn.xk.chatBack.model;

import com.corundumstudio.socketio.AckCallback;
import io.netty.util.internal.PlatformDependent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-23
 */
public class AckEntry {

    final Map<Long, AckCallback<?>> ackCallbacks = PlatformDependent.newConcurrentHashMap();
    final AtomicLong ackIndex = new AtomicLong(-1);

    public long registerAckCallback(AckCallback<?> callback) {
        long index = ackIndex.incrementAndGet();
        ackCallbacks.put(index, callback);
        return index;
    }

    public long registerAckCallback(long index, AckCallback<?> callback) {
        ackCallbacks.put(index, callback);
        return index;
    }

    public Set<Long> getAckIndexes() {
        return ackCallbacks.keySet();
    }

    public AckCallback<?> getAckCallback(long index) {
        return ackCallbacks.get(index);
    }

    public AckCallback<?> cancelCallback(long index) {
        return ackCallbacks.remove(index);
    }

    public void initAckIndex(long index) {
        ackIndex.compareAndSet(-1, index);
    }

}
