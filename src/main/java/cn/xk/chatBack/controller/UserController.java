package cn.xk.chatBack.controller;

import cn.xk.chatBack.model.ChatGroup;
import cn.xk.chatBack.model.LoginUser;
import cn.xk.chatBack.model.R;
import cn.xk.chatBack.model.User;
import cn.xk.chatBack.model.constant.HttpStatus;
import cn.xk.chatBack.service.UserService;
import cn.xk.chatBack.utils.AliOSSUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import javax.websocket.server.PathParam;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@CrossOrigin
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private AliOSSUtils aliOSSUtils;

    @PatchMapping("/username")
    public R<?> changeUserName(@RequestBody User user){
        int result = userService.changeUserName(user);
        return result>0 ? R.ok(user,"修改成功") : R.fail("修改失败");
    }

    @PatchMapping("/password")
    public R<?> changePassword(@RequestBody User user,@RequestParam String password){
        user.setPassWord(password);
        int result = userService.changePassword(user);
        return result>0 ? R.ok(user,"修改成功") : R.fail("修改失败");
    }

    //todo
    @DeleteMapping("/deleteUser")
    public void deleteUser(){
    }

    @GetMapping("/findByName")
    public R<List<User>> getUsersByName(String username){
        R<List<User>> r = new R<>();
        r.setData(userService.getUsersByName(username));
        r.setCode(HttpStatus.SUCCESS);
        r.setMsg("操作成功");
        return r;
    }

    @PostMapping("/avatar")
    public R<?> uploadAvatar(@RequestParam("avatar") MultipartFile img) throws IOException {
//        Resource resource = resourceLoader.getResource("classpath:/avatar/");
//        String path = resource.getFile().getAbsolutePath()+"\\";
        String fileName = img.getOriginalFilename();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        fileName = uuid+fileName;
//        path += fileName;
//        img.transferTo(new File(path));
        String avatarUrl = aliOSSUtils.upload(img);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        User user = loginUser.getUser();
        //user.setAvatar("/api/avatar/"+fileName);
        user.setAvatar(avatarUrl);
        int result = userService.updateUser(user);
        return R.ok(user,"修改头像成功");
    }


}
