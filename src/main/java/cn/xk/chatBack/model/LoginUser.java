package cn.xk.chatBack.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author xk
 */
@Data
@NoArgsConstructor
public class LoginUser implements UserDetails {

    private User user;

    private List<String> permissions;

    /**
     * 保存到Redis中会避免乱码问题
     */
    @JSONField(serialize = false)
    private List<SimpleGrantedAuthority> authorities;

    /**
     * 两个参数的构造方法
     * @param user
     * @param permissions
     */
    public LoginUser(User user, List<String> permissions) {
        this.user = user;
        this.permissions = permissions;
    }

    private long tokenExpiredTime;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities != null) {
            return authorities;
        }
        //把permissions中String类型的权限信息封装成SimpleGrantedAuthority对象
        authorities = new ArrayList<>();
        for (String permission : permissions) {
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(permission);
            authorities.add(authority);
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassWord();
    }

    @Override
    public String getUsername() {
        return user.getUserName();
    }

    @Override//账户是否过期
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账户是否锁定
     * @return
     */
    @Override
    public boolean isAccountNonLocked() {
        //先改成true
        return true;
    }

    /**
     * 凭证是否过期
     * @return
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 账户是否可用
     * @return
     */
    @Override
    public boolean isEnabled() {
        return (user.getStatus().equals("on"));
    }


}
