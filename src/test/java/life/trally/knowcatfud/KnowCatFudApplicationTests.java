package life.trally.knowcatfud;

import life.trally.knowcatfud.pojo.entity.User;
import life.trally.knowcatfud.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@SpringBootTest
class KnowCatFudApplicationTests {

    @Autowired
    DataSource dataSource;

    @Test
    void contextLoads() throws SQLException {
        System.out.println(dataSource.getClass());
        Connection connection = dataSource.getConnection();
        System.out.println(connection);
        connection.close();
    }

    @Autowired
    private UserMapper userMapper;

    @Test
    void userMapperTest() {
        List<User> users = userMapper.selectList(null);
        users.forEach(System.out::println);
    }


    @Autowired
    private PasswordEncoder passwordEncoder;
    @Test
    void passwordTest() {
        String encode = passwordEncoder.encode("pwd1");
        System.out.println(encode);
    }


}
