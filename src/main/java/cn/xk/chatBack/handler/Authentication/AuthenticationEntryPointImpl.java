package cn.xk.chatBack.handler.Authentication;


import cn.xk.chatBack.model.R;
import cn.xk.chatBack.utils.WebUtils;
import com.alibaba.fastjson.JSON;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author xk
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        authException.printStackTrace();
        R<?> result = R.fail("用户认证失败请重新登录");
        result.setCode(401);
        String json = JSON.toJSONString(result);
        //处理异常
        WebUtils.renderString(response, json);
    }
}
