package life.trally.knowcatfud.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import life.trally.knowcatfud.dao.UserFileMapper;
import life.trally.knowcatfud.dao.UserMapper;
import life.trally.knowcatfud.jwt.LoginUser;
import life.trally.knowcatfud.pojo.User;
import life.trally.knowcatfud.pojo.UserFile;
import life.trally.knowcatfud.service.interfaces.UserService;
import life.trally.knowcatfud.utils.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final AuthenticationManager authenticationManager;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final UserFileMapper userFileMapper;

    public UserServiceImpl(AuthenticationManager authenticationManager, UserMapper userMapper, PasswordEncoder passwordEncoder, UserFileMapper userFileMapper) {
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.userFileMapper = userFileMapper;
    }


    @Override
    public String login(User user) {  // 用户认证
        UsernamePasswordAuthenticationToken usernameAndPassword = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        Authentication authenticate;
        try {
            authenticate = authenticationManager.authenticate(usernameAndPassword);// 进行认证. spring security会自动调用UserDetailsService的实现类
        } catch (Exception e) {
            return null; // 登陆失败
        }
        LoginUser principle = (LoginUser) authenticate.getPrincipal();

        Map<String, Object> claims = Map.of(
                "other", "");

        /*
         * clams:
         * subject: LoginUser
         */

        String jwt = JwtUtil.generateToken(claims, JSON.toJSONString(principle));

        return jwt;
    }

    @Override
    public Result reg(User user) {  // 用户注册
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername());

        if (userMapper.selectOne(queryWrapper) == null) {  // 用户不存在
            User userInsert = new User(null, user.getUsername(), passwordEncoder.encode(user.getPassword()));
            userMapper.insert(userInsert);
            userFileMapper.insert(UserFile.rootDir(user.getUsername()));
            return Result.SUCCESS;
        } else {
            return Result.USER_ALREADY_EXIST;
        }

        // return RegResult.FAIL;
    }


}
