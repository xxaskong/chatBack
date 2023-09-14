package cn.xk.chatBack.model.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserMessageVo extends UserMessage {

    private double width;

    private double height;

    private ByteBuffer imgContent;



}
