package cn.xk.chatBack.service.Impl;


import cn.xk.chatBack.mapper.MenuMapper;
import cn.xk.chatBack.mapper.UserMapper;
import cn.xk.chatBack.model.LoginUser;
import cn.xk.chatBack.model.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


/**
 * @author xk
 */
@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MenuMapper menuMapper;

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        // 查询用户信息
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("user_name",userName);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            // 异常信息会被ExceptionTranslationFilter捕获到
            throw new RuntimeException("用户不存在");
        }
        // 授权
        // todo 这里可以用权限集来做
        List<String> list = menuMapper.selectPermsByUserId(user.getUserId());

        // 数据封装成UserDetails返回
        return new LoginUser(user, list);
    }
}
