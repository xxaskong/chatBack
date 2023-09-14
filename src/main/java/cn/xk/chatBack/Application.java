package cn.xk.chatBack;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import javax.annotation.Resource;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("cn.xk.chatBack.mapper")
public class Application implements CommandLineRunner {
    public static void main(String[] args) {
        log.info("NettySocketioConfig线程名称:{}",Thread.currentThread().getId());
        SpringApplication.run(Application.class,args);
    }

    @Resource
    private SocketIOServer socketIOServer;

    @Override
    public void run(String... strings) {
        socketIOServer.start();
        log.info("socket.io启动成功！");
    }
}
