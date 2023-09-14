package cn.xk.chatBack.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户认证请求包装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserRequestVo {

    @JsonProperty(value = "username")
    String userName;

    @JsonProperty(value = "password")
    String passWord;
}
