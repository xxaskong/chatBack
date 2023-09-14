package cn.xk.chatBack.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author xk
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "user")
public class User  implements Serializable {

    /**
     * 用户id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String userId;

    /**
     * 用户名
     */
    @JsonProperty(value = "username")
    private String userName;

    /**
     * 密码
     */
    @JsonIgnore
    @JsonProperty(value = "password")
    private String passWord;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 角色
     */
    private String role;

    /**
     *  用户状态
     */
    private String status;

    /**
     * 标签
     */
    private String tag;

    /**
     * 创建时间
     */
    private long createTime;

    private static final long serialVersionUID = -40356785423868312L;

}
