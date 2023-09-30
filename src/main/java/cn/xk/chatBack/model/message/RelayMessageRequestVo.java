package cn.xk.chatBack.model.message;

import cn.xk.chatBack.model.R;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author: xk
 * @Description:
 * @CreateDate: Created in 2023-09-26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RelayMessageRequestVo {

    /**
     * 接收方类型
     */
    String targetType;

    /**
     * 接收方id
     */
    String targetId;

    /**
     * 包装过后的message
     */
    R<Object> message;

    public RelayMessageRequestVo(String json) throws JsonProcessingException {
        RelayMessageRequestVo relayMessageRequestVo = new ObjectMapper().readValue(json, RelayMessageRequestVo.class);
        this.message = relayMessageRequestVo.getMessage();
        this.targetId = relayMessageRequestVo.getTargetId();
        this.targetType = relayMessageRequestVo.getTargetType();
    }

}
