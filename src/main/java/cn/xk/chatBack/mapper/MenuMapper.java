package cn.xk.chatBack.mapper;

import cn.xk.chatBack.model.Menu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author xk
 */
@Repository
public interface MenuMapper extends BaseMapper<Menu> {

    List<String> selectPermsByUserId(@Param("userId") String userId);

}
