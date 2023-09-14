package cn.xk.chatBack.config;


import cn.xk.chatBack.filter.JwtAuthenticationTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Resource;

/**
 * @author xk
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, jsr250Enabled = true)//注解开启相关配置
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;


    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;


    @Resource
    private AccessDeniedHandler accessDeniedHandler;

    @Resource
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        //用户输入的密码, 和数据库中的密码的密文进行比较
        //所以数据库中存储的密码使用这个加密后的密文
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // 关闭csrf, 好像是因为前后端分离
                .csrf().disable()
                // 不通过session获取SecurityContext
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                // 允许登录接口匿名访问
                //.antMatchers("/auth/User/login").anonymous()
                // 除上面之外所有请求要进行认证
                .anyRequest().authenticated();

        http.cors();

        // 配置认证过滤器
        http.addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
        // 配置异常处理器
        http
                .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        //自定义provider及service，一套身份认证
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }


}
