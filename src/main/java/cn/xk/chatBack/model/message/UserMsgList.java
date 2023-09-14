package cn.xk.chatBack.model.message;

import cn.xk.chatBack.model.User;
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
public class UserMsgList extends User {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<UserMessage> messages;

}
