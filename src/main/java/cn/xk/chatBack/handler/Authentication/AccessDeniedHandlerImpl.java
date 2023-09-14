package cn.xk.chatBack.handler.Authentication;

import cn.xk.chatBack.model.R;
import cn.xk.chatBack.utils.WebUtils;
import com.alibaba.fastjson2.JSON;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author xk
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        R<?> result = R.fail("未授权");
        String json = JSON.toJSONString(result);
        //处理异常
        WebUtils.renderString(response, json);
    }

}
