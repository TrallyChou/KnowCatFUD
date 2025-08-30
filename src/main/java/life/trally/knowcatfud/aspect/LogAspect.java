package life.trally.knowcatfud.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Slf4j
public class LogAspect {

    @Around("execution(* life.trally.knowcatfud.controller.*.*(..))")
    public Object controllerLog(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = null;
        if (log.isDebugEnabled()) {
            log.debug("[Controller]{}入参: {}", joinPoint.getSignature().toShortString(), joinPoint.getArgs());
            try {
                result = joinPoint.proceed();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            log.debug("[Controller]{}结果:{}", joinPoint.getSignature().toShortString(), result);
        }
        return result;
    }

    @Around("execution(* life.trally.knowcatfud.service.impl.*.*(..))")
    public Object serviceLog(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = null;

        log.debug("[Service]{}入参: {}", joinPoint.getSignature().toShortString(), joinPoint.getArgs());
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            log.error("[Service]发生异常", e);
            return null;   // 后续将Service的Result一致更换为ServiceResult<>
        }
        log.debug("[Service]{}结果: {}", joinPoint.getSignature().toShortString(), result);

        return result;
    }

}
