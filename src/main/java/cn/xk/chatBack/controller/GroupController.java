package cn.xk.chatBack.controller;

import cn.xk.chatBack.model.*;
import cn.xk.chatBack.model.message.GroupMessage;
import cn.xk.chatBack.model.message.MessagePageRequestVo;
import cn.xk.chatBack.service.GroupService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-08-19
 */
@CrossOrigin
@Slf4j
@RestController
@RequestMapping("/group")
public class GroupController {

    @Resource
    GroupService groupService;



    @GetMapping("/findByName")
    public R<List<ChatGroup>> getGroupsByName(String groupName){
        List<ChatGroup> chatGroupList = groupService.getGroupsByName(groupName);
        return R.ok(chatGroupList);
    }

    @GetMapping("/groupMessages")
    public R<Map<String, Object>> groupMessages(MessagePageRequestVo messagePageRequestVo) {
        messagePageRequestVo.
                setCurrent(messagePageRequestVo.getCurrent()/30+1);
        Page<GroupMessage> userMessagePage = groupService.selectPages(messagePageRequestVo);
        List<User> userList = groupService.getUsersInGroup(messagePageRequestVo.getGroupId());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("messageArr", userMessagePage.getRecords());
        resultMap.put("userArr", userList);
        return R.ok(resultMap);
    }

}
