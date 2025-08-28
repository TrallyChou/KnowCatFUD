package life.trally.knowcatfud.utils;

import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class TimeUtils {

    @NotNull
    public static Boolean expired(@NotNull Timestamp createdAt, long duration, ChronoUnit unit) {
        return Instant.now().isAfter(createdAt.toInstant().plus(duration, unit));
    }
}
