package life.trally.knowcatfud.config.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import life.trally.knowcatfud.pojo.jwt.LoginUser;
import life.trally.knowcatfud.utils.JsonUtils;
import life.trally.knowcatfud.utils.JwtUtils;
import life.trally.knowcatfud.utils.RedisUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final RedisUtils redisUtils;

    public JwtAuthenticationTokenFilter(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization"); // token
//        System.out.println(authorization);

        try {
            if (redisUtils.exists("logout:" + authorization)) {
                throw new RuntimeException();
            }
            Claims claims = JwtUtils.parseToken(authorization);
            String loginUserStr = claims.getSubject();
            LoginUser loginUser = JsonUtils.deserialize(loginUserStr, LoginUser.class);
//            System.out.println(loginUser);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            // 两个参数表示未认证，三个参数表示已认证

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            filterChain.doFilter(request, response);  // 因为jwt解析失败的访问没有得到认证，并不会通过后面的过滤器，所以这里不考虑拦截
        }


    }
}
