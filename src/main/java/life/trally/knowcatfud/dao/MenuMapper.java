package life.trally.knowcatfud.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import life.trally.knowcatfud.pojo.Menu;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MenuMapper extends BaseMapper<Menu> {

    @Select("select perm from sys_menu t1\n" +
            "    inner join sys_role_menu t2 on t1.id = t2.menu_id\n" +
            "    inner join sys_role t3 on t3.id = t2.role_id\n" +
            "    inner join sys_user_role t4 on t4.role_id = t3.id\n" +
            "    inner join users t5 on t5.id = t4.user_id\n" +
            "    where t5.id=#{id}")
    List<String> getPermsByUserId(long id);
}
