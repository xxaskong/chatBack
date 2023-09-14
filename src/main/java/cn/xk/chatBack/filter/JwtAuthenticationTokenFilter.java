package cn.xk.chatBack.filter;

import cn.xk.chatBack.model.LoginUser;
import cn.xk.chatBack.model.User;
import cn.xk.chatBack.utils.RedisCache;
import cn.xk.chatBack.utils.jwt.JwtUtil;
import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * @author xk
 */
@Slf4j
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Resource
    RedisCache redisCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader("token");
        if (!StringUtils.hasText(token)) {
            //放行让请求去请求不需要权限的接口
            AbstractAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(new LoginUser(new User(), null), null, null);
            //存入SecurityContextHolder
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
            //在请求完成后不执行下面的逻辑代码
            return;
        }
        log.info("filter:{}",token);
        //解析token
        String json;
        String id;
        try {
            Claims claims = JwtUtil.parseJWT(token);
            json = claims.getSubject();
            Map<String, Object> map = JSON.parseObject(json, Map.class);
            id = (String) map.get("id");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("token非法");
        }
        //从redis中获取用户信息
        UserDetails loginUser;
        loginUser = redisCache.getCacheObject("chatLogin:" + id);

        if (Objects.isNull(loginUser)) {
            throw new RuntimeException("Id非法");
        }

        //放入权限
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());

        //存入SecurityContextHolder
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request, response);

    }

}
