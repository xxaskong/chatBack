package cn.xk.chatBack.model.oss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component // 交给IOC容器
@ConfigurationProperties(prefix = "aliyun.oss") // 需要引入注解处理器依赖(可选操作)
public class AliOSSProperties {

    private String endpoint;   //阿里云OSS域名
    private String accessKeyId;   //用户身份ID
    private String accessKeySecret;  //用户密钥
    private String bucketName;    //存储空间的名字

}

