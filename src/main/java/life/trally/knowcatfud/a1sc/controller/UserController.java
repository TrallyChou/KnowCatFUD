package life.trally.knowcatfud.a1sc.controller;

import io.swagger.v3.oas.annotations.Operation;
import life.trally.knowcatfud.a1sc.service.interfaces.UserService;
import life.trally.knowcatfud.mapping.RequestMapping;
import life.trally.knowcatfud.pojo.request.LoginRequest;
import life.trally.knowcatfud.pojo.request.RegRequest;
import life.trally.knowcatfud.pojo.response.LoginResponse;
import life.trally.knowcatfud.utils.RedisUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {
    private final UserService userService;
    private final RequestMapping requestMapping;
    private final RedisUtils redisUtils;

    public UserController(UserService userService, RequestMapping requestMapping, RedisUtils redisUtils) {
        this.userService = userService;
        this.requestMapping = requestMapping;
        this.redisUtils = redisUtils;
    }

    @PostMapping("/login")
    @Operation(summary = "登录")
    public R<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        String jwt = userService.login(requestMapping.toUser(loginRequest));
        if (StringUtils.hasLength(jwt)) {
            return R.ok(new LoginResponse(jwt)).message("登录成功");
        }
        return R.error("登录失败");
    }

    @PostMapping("/reg")
    @Operation(summary = "注册")
    public R<Void> reg(@RequestBody RegRequest regRequest) {
        return switch (userService.reg(requestMapping.toUser(regRequest))) {
            case USER_ALREADY_EXIST -> R.error("用户已存在");
            case SUCCESS -> R.ok("注册成功");
            case FAIL -> R.error("注册失败");
        };
    }

    @PostMapping("/logout")   // 该API不要放行未登录用户
    @Operation(summary = "退出登录")
    public R<Void> logout(
            @RequestHeader String Authorization
    ) {
        return switch (userService.logout(Authorization)) {
            case SUCCESS -> R.ok();
            default -> R.error();
        };

    }


}
