package cn.xk.chatBack.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-08-23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "user_map")
public class UserRelationship {

    @TableId(type = IdType.AUTO)
    private int id;

    private String userId;

    private String friendId;
}
