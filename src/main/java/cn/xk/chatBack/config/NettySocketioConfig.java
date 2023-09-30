package cn.xk.chatBack.config;

import cn.xk.chatBack.model.AckEntry;
import cn.xk.chatBack.model.RetryQueue;
import cn.xk.chatBack.model.connect.ActiveGroupUser;
import cn.xk.chatBack.model.connect.UserSocketPool;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@Slf4j
public class NettySocketioConfig {
    /**
     * netty-socketio服务器
     */

//    @Resource
//    private ApplicationArguments applicationArguments;

    @Resource
    private ApplicationContext applicationContext;

    @Value("${wsPort}")
    private int wsPort;

    @Bean
    public SocketIOServer socketIOServer() {
        //List<String> nonOptionArgs = applicationArguments.getNonOptionArgs();

        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        //List<String> nonOptionArgs1 = applicationArguments.getNonOptionArgs();
        System.out.println("xxaskong");
        System.out.println(wsPort);
        config.setPort(wsPort);
        config.setPingInterval(25000);
        config.setPingTimeout(5000);
        config.setMaxFramePayloadLength(1024 * 1024);
        config.setMaxHttpContentLength(1024 * 1024);

        SocketIOServer server = new SocketIOServer(config);
        return server;
    }

    /**
     * 用于扫描netty-socketio的注解，比如 @OnConnect、@OnEvent
     */
    @Bean
    public SpringAnnotationScanner springAnnotationScanner() {
        return new SpringAnnotationScanner(socketIOServer());
    }

    @Bean
    public ActiveGroupUser activeGroupUser() {
        log.info("NettySocketioConfig线程名称:{}",Thread.currentThread().getId());

        return new ActiveGroupUser();
    }

    @Bean
    public UserSocketPool userSocketPool() {
        return new UserSocketPool();
    }

    @Bean
    public AckEntry ackEntry(){
        return new AckEntry();
    }

    @Bean
    public RetryQueue retryQueue(){
        return new RetryQueue();
    }
}
