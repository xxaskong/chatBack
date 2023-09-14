package cn.xk.chatBack.config;

import cn.xk.chatBack.model.connect.ActiveGroupUser;
import cn.xk.chatBack.model.connect.UserSocketPool;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
public class NettySocketioConfig {
    /**
     * netty-socketio服务器
     */

    @Resource
    private ApplicationArguments applicationArguments;

    @Resource
    private ApplicationContext applicationContext;

    @Bean
    public SocketIOServer socketIOServer() {
        List<String> nonOptionArgs = applicationArguments.getNonOptionArgs();

        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("localhost");
        List<String> nonOptionArgs1 = applicationArguments.getNonOptionArgs();
        System.out.println("xxaskong");
        System.out.println(Arrays.toString(nonOptionArgs1.toArray()));
        config.setPort(Integer.parseInt(applicationArguments.getNonOptionArgs().get(0).split("=")[1]));
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
}
