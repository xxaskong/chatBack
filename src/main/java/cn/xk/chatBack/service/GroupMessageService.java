package cn.xk.chatBack.service;

import cn.xk.chatBack.model.R;
import cn.xk.chatBack.model.message.GroupMessage;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-06
 */
public interface GroupMessageService {

    int insertGroupMessage(GroupMessage groupMessage);

    public R<?> sendGroupMessage(String groupId,R<Object> groupMessage);

    public R<?> sendEvent(String name, String groupId, Object o);
}
