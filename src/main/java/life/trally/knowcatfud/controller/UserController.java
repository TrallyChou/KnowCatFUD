package life.trally.knowcatfud.controller;

import life.trally.knowcatfud.mapping.RequestMapping;
import life.trally.knowcatfud.request.LoginRequest;
import life.trally.knowcatfud.request.RegRequest;
import life.trally.knowcatfud.response.LoginResponse;
import life.trally.knowcatfud.service.interfaces.UserService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private final UserService userService;
    private final RequestMapping requestMapping;

    public UserController(UserService userService, RequestMapping requestMapping) {
        this.userService = userService;
        this.requestMapping = requestMapping;
    }

    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        String jwt = userService.login(requestMapping.toUser(loginRequest));
        if (StringUtils.hasLength(jwt)) {
            return R.ok(new LoginResponse(jwt)).message("登录成功");
        }
        return R.error("登录失败");
    }

    @PostMapping("/reg")
    public R<Void> reg(@RequestBody RegRequest regRequest) {
        return switch (userService.reg(requestMapping.toUser(regRequest))) {
            case USER_ALREADY_EXIST -> R.error("用户已存在");
            case SUCCESS -> R.ok("注册成功");
            case FAIL -> R.error("注册失败");
        };
    }

}
