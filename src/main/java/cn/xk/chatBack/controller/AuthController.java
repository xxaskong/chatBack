package cn.xk.chatBack.controller;

import cn.xk.chatBack.model.AuthUserRequestVo;
import cn.xk.chatBack.model.R;
import cn.xk.chatBack.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xk
 * @description 用户认证类
 * @createDate 2023-08-18
 */
@CrossOrigin
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    UserService userService;

    @PostMapping("/login")
    public R<?> login(@RequestBody AuthUserRequestVo authUserRequestVo) {
        return R.ok(userService.login(authUserRequestVo));
    }

    /**
     *  注册并登录
     *
     * @param authUserRequestVo
     * @return
     */
    @PostMapping("/register")
    public R<?> registerter(@RequestBody AuthUserRequestVo authUserRequestVo) {
        int result = userService.register(authUserRequestVo);
        Map<String, Object> map = null;
        if(result > 0) {
            map = userService.login(authUserRequestVo);
        }
        return result>0 ? R.ok(map) : R.fail("注册失败");
    }
}
