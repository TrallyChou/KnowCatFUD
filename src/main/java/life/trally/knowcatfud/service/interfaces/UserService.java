package life.trally.knowcatfud.service.interfaces;

import com.baomidou.mybatisplus.extension.service.IService;
import life.trally.knowcatfud.pojo.User;

public interface UserService extends IService<User> {

    // 注册结果成功情况
    enum Result {
        USER_ALREADY_EXIST,
        SUCCESS,
        FAIL
    }

    String login(User user);

    Result reg(User user);
}
