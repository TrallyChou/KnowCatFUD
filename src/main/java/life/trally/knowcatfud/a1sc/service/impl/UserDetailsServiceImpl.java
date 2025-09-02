package life.trally.knowcatfud.a1sc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import life.trally.knowcatfud.mapper.MenuMapper;
import life.trally.knowcatfud.mapper.UserMapper;
import life.trally.knowcatfud.pojo.jwt.LoginUser;
import life.trally.knowcatfud.pojo.entity.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;


// authenticationManager.authenticate(token)方法会调用loadUserByUsername方法
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    private final MenuMapper menuMapper;

    public UserDetailsServiceImpl(UserMapper userMapper, MenuMapper menuMapper) {
        this.userMapper = userMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {

        // 连接数据库，获取账户信息
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        List<String> authorities = menuMapper.getPermsByUserId(user.getId());


        return new LoginUser(user, authorities);  // 返回包含账号密码的UserDetails对象，用户输入密码来源于authenticationManager.authenticate(usernameAndPassword)，由Spring Security自动比对
    }
}
