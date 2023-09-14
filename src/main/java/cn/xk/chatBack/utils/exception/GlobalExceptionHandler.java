package cn.xk.chatBack.utils.exception;



import cn.xk.chatBack.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author xk
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public R<?> error(Exception e) {
        e.printStackTrace();
        log.error("执行了全局异常处理"+e.getMessage());
        return R.fail("执行了全局异常处理");
    }

    @ExceptionHandler(ArithmeticException.class)
    @ResponseBody
    public R<?> error(ArithmeticException e) {
        log.error("执行了特定异常处理"+e.getMessage());
        return R.fail("执行了特定异常处理");
    }

    @ExceptionHandler(XkException.class)
    @ResponseBody
    public R<?> error(XkException e) {
        log.error("xxaskong 异常捕获"+e.getMessage());
        return R.fail(e.getCode(), e.getMsg());
    }
}
