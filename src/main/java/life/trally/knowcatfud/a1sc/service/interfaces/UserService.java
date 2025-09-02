package life.trally.knowcatfud.a1sc.service.interfaces;

import com.baomidou.mybatisplus.extension.service.IService;
import life.trally.knowcatfud.pojo.entity.User;

public interface UserService extends IService<User> {

    // 注册结果成功情况
    enum Result {
        USER_ALREADY_EXIST,
        SUCCESS,
        FAIL
    }

    String login(User user);

    Result reg(User user);

    Result logout(String jwt);
}
