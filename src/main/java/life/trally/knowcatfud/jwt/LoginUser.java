package life.trally.knowcatfud.jwt;

import com.alibaba.fastjson2.annotation.JSONField;
import life.trally.knowcatfud.pojo.User;
import lombok.Data;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
public class LoginUser implements UserDetails {
    private User user;
    private List<String> list;   // 权限列表 坑：千万不要把这个List命名为authorities，否则会被lombok的@Data重写getAuthorities

    @JSONField(serialize = false)
    private List<SimpleGrantedAuthority> authorities;  //

    public LoginUser(User user, List<String> list) {
        this.user = user;
        this.list = list;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(authorities != null){
            return authorities;
        }
        authorities =new ArrayList<>();
        list.forEach(perm -> this.authorities.add(new SimpleGrantedAuthority(perm)));
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
