package life.trally.knowcatfud.config.filter;

import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import life.trally.knowcatfud.jwt.LoginUser;
import life.trally.knowcatfud.utils.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
//    private final HttpServletResponse httpServletResponse;

//    public JwtAuthenticationTokenFilter(HttpServletResponse httpServletResponse) {
//        this.httpServletResponse = httpServletResponse;
//    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        String authorization = request.getHeader("Authorization"); // token
//        System.out.println(authorization);

        try {
            Claims claims = JwtUtil.parseToken(authorization);
            String loginUserStr = claims.getSubject();
            LoginUser loginUser = JSON.parseObject(loginUserStr, LoginUser.class);
//            System.out.println(loginUser);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            // 两个参数表示未认证，三个参数表示已认证

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            filterChain.doFilter(request, response);  // 因为jwt解析失败的访问并不会通过后面的过滤器，所以这里不考虑拦截
        }


    }
}
