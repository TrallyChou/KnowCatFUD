package life.trally.knowcatfud.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import javax.crypto.SecretKey;
import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class JwtUtil {
    // 使用安全的密钥生成方式
    private static final Key SECRET_KEY;

    static {
        // 从环境变量获取密钥（推荐生产环境使用）
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isEmpty()) {
            // 开发环境使用默认密钥（生产环境应避免）
            secret = "default-secret-key-with-minimum-256-bits-length-required-here";
        }
        SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private static final long DEFAULT_EXPIRATION_HOURS = 24;

    // 生成JWT
    public static String generateToken(Map<String, Object> claims, String subject) {
        return generateToken(claims, subject, DEFAULT_EXPIRATION_HOURS, TimeUnit.HOURS);
    }

    public static String generateToken(Map<String, Object> claims, String subject,
                                       long duration, TimeUnit unit) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + unit.toMillis(duration));

        return Jwts.builder()
//                .claims(claims) // 新版API使用.claims()方法
                .subject(subject)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(SECRET_KEY) // 指定签名算法
                .compact();
    }

    // 解析JWT（使用最新API）
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 验证JWT（带详细异常处理）
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (SignatureException ex) {
            // 签名无效
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            // JWT已过期
        } catch (io.jsonwebtoken.MalformedJwtException ex) {
            // JWT格式错误
        } catch (io.jsonwebtoken.UnsupportedJwtException ex) {
            // 不支持的JWT
        } catch (IllegalArgumentException ex) {
            // 空或null token
        }
        return false;
    }

    // 获取过期时间
    public static Date getExpirationDate(String token) {
        return parseToken(token).getExpiration();
    }

    // 刷新令牌
    public static String refreshToken(String token, long newDuration, TimeUnit unit) {
        Claims claims = parseToken(token);
        return generateToken(claims, claims.getSubject(), newDuration, unit);
    }
}
