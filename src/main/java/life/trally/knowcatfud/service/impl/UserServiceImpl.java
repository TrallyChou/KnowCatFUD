package life.trally.knowcatfud.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import life.trally.knowcatfud.dao.UserMapper;
import life.trally.knowcatfud.jwt.LoginUser;
import life.trally.knowcatfud.pojo.User;
import life.trally.knowcatfud.service.interfaces.UserService;
import life.trally.knowcatfud.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public String login(User user) {  // 用户认证
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        Authentication authenticate;
        try {
            authenticate = authenticationManager.authenticate(token);// 进行认证. spring security会自动调用UserDetailsService的实现类
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

//        System.out.println(jwt);

        return jwt;
    }

    @Override
    public Result reg(User user) {  // 用户注册
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername());

        if (userMapper.selectOne(queryWrapper) == null) {  // 用户不存在
            User userInsert = new User(null, user.getUsername(), passwordEncoder.encode(user.getPassword()));
            userMapper.insert(userInsert);
            return Result.SUCCESS;
        } else {
            return Result.USER_ALREADY_EXIST;
        }

        // return RegResult.FAIL;
    }

}
