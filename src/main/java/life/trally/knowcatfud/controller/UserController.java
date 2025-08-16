package life.trally.knowcatfud.controller;

import life.trally.knowcatfud.pojo.User;
import life.trally.knowcatfud.service.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public R login(@RequestBody User user) {
        String jwt = userService.login(user);
        if(StringUtils.hasLength(jwt)){
            return R.ok().message("登录成功").data("token",jwt);
        }
        return R.error().message("登录失败");
    }

    @PostMapping("/reg")
    public R reg(@RequestBody User user) {
        return switch (userService.reg(user)) {
            case USER_ALREADY_EXIST -> R.error().message("用户已存在");
            case SUCCESS -> R.ok().message("注册成功");
            case FAIL -> R.error().message("注册失败");
        };
    }

}
