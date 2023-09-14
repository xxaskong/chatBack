package cn.xk.chatBack.config;

import com.hazelcast.config.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;


/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-08
 */
@Configuration
@ImportResource(locations = "classpath:hazelcast.xml")
public class HazelcastConfiguration {
}
