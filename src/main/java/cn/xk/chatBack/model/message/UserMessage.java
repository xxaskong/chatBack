package cn.xk.chatBack.model.message;

import cn.xk.chatBack.model.message.Message;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-08-19
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "friend_message")
public class UserMessage extends Message {

    @TableId(type = IdType.AUTO)
    private int id;

    /**
     * 发送方id
     */
    private String userId;

    /**
     * 接收方id
     */
    private String friendId;

    /**
     * 内容
     */
    private String content;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 时间
     */
    private long time;

}
