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
import java.util.Set;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
//    private final HttpServletResponse httpServletResponse;
    private static final Set<String> ALLOWED_PATHS = Set.of("/login", "/reg");

//    public JwtAuthenticationTokenFilter(HttpServletResponse httpServletResponse) {
//        this.httpServletResponse = httpServletResponse;
//    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // 放行一些请求
        if (ALLOWED_PATHS.contains(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization"); // token
//        System.out.println(authorization);

        try {
            Claims claims = JwtUtil.parseToken(authorization);
            String loginUserStr = claims.getSubject();
            LoginUser loginUser = JSON.parseObject(loginUserStr, LoginUser.class);
//            System.out.println(loginUser);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, null);
            // 两个参数表示未认证，三个参数表示已认证

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
