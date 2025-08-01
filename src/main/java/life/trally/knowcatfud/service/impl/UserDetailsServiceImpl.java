package life.trally.knowcatfud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import life.trally.knowcatfud.jwt.LoginUser;
import life.trally.knowcatfud.dao.UserMapper;
import life.trally.knowcatfud.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


// authenticationManager.authenticate(token)方法会调用loadUserByUsername方法
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) {

        // 连接数据库，获取账户信息
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);
        if(user==null){
            throw new UsernameNotFoundException("用户不存在");
        }

        //TODO: 查询用户对应的权限信息

        return new LoginUser(user);  // 返回包含账号密码的UserDetails对象，由Spring Security自动比对
    }
}
