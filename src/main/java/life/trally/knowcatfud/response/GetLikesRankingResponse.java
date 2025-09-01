package life.trally.knowcatfud.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

@Data
@AllArgsConstructor
public class GetLikesRankingResponse {
    Set<ZSetOperations.TypedTuple<String>> ranking;
}
