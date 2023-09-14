package cn.xk.chatBack.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName(value = "chat_group")
public class ChatGroup {

    /**
     * 组id
     */
    @TableId("group_id")
    private String groupId;

    /**
     * 创建建用户id
     */
    private String userId;

    /**
     * 群名
     */
    private String groupName;

    /**
     * 群公告
     */
    private String notice;

    /**
     * 创建时间
     */
    private long createTime;
}
