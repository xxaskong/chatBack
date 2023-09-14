package cn.xk.chatBack.controller;

import cn.xk.chatBack.model.message.MessagePageRequestVo;
import cn.xk.chatBack.model.R;
import cn.xk.chatBack.model.message.UserMessage;
import cn.xk.chatBack.service.FriendMessageService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: xk
 * @Description: 发送给用户的消息
 * @CreateDate: Created in 2023-08-19
 */
@CrossOrigin
@Slf4j
@RestController
@RequestMapping("/friend")
public class    FriendMessageController {

    @Resource
    FriendMessageService friendMessageService;

    @GetMapping("/friendMessages")
    public R<Map<String, Object>> friendMessages( MessagePageRequestVo messagePageRequestVo) {
        messagePageRequestVo.setCurrent(messagePageRequestVo.getCurrent()/30+1);
        Page<UserMessage> userMessagePage = friendMessageService.selectPages(messagePageRequestVo);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("messageArr",userMessagePage.getRecords());
        return R.ok(resultMap);
    }
}
