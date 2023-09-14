package cn.xk.chatBack.model.message;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-08-19
 */
@Data
public class Message {

    /**
     * 发送目标类型（群或用户）
     */
    @JsonIgnore
    @TableField(exist = false)
    String targetType;
}
