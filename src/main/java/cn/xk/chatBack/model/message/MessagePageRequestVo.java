package cn.xk.chatBack.model.message;

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
public class MessagePageRequestVo {

    /**
     *  组id
     */
    private String groupId;

    /**
     * 发送方id
     */
    private String userId;

    /**
     * 接收方id
     */
    private String friendId;

    /**
     * 当前页码
     */
    private int current;

    /**
     * 每页数据量
     */
    private int pageSize;
}
