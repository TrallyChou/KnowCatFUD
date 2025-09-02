package life.trally.knowcatfud.utils;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtils {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisUtils(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void hSet(String key, String field, String value) {
        if (value != null) {
            redisTemplate.opsForHash().put(key, field, value);
        } else {
            redisTemplate.opsForHash().delete(key, field);   // 直接存入null会抛出异常
        }
    }

    public String hGet(String key, String field) {
        return (String) redisTemplate.opsForHash().get(key, field);
    }

    public Map<String, String> hGetAll(String key) {
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        return hashOps.entries(key);
    }

    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    public void expire(String key, long timeout) {
        redisTemplate.expire(key, timeout, TimeUnit.HOURS);
    }

    public void zAdd(String key, String value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }

    public Double zScore(String key, String value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    public void zDel(String key, String value) {
        redisTemplate.opsForZSet().remove(key, value);
    }

    public void zIncrby(String key, String value, int delta) {
        redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    public void sAdd(String key, String value) {
        redisTemplate.opsForSet().add(key, value);
    }

    public void sDel(String key, String value) {
        redisTemplate.opsForSet().remove(key, value);
    }

    public void sAdd(String key, List<String> value) {
        redisTemplate.opsForSet().add(key, value.toArray(new String[0]));
    }

    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    public Boolean sIsMember(String key, String value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    public Set<ZSetOperations.TypedTuple<String>> zRevRangeWithScore(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

}
