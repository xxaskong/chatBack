package cn.xk.chatBack.model.message;

import cn.xk.chatBack.model.ChatGroup;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-08-22
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatGroupMsgList extends ChatGroup {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<GroupMessage> messages;

}
