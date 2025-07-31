package life.trally.knowcatfud.utils;

import com.alibaba.fastjson2.JSON;
import life.trally.knowcatfud.jwt.LoginUser;

public class AccessCheckUtil {
    public static boolean checkAccess(String token, String username) {

        try {
            LoginUser user = JSON.parseObject(JwtUtil.parseToken(token).getSubject(), LoginUser.class);
            return user.getUsername().equals(username);
        } catch (Exception e) {
            return false;

        }

    }
}
