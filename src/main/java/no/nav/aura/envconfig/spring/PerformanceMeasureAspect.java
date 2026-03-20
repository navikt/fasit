package no.nav.aura.envconfig.spring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceMeasureAspect {

    private final MeterRegistry meterRegistry;

    public PerformanceMeasureAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around(value = "execution(* no.nav.aura.envconfig.FasitRepository.*(..))")
    public Object checkIntegrity(ProceedingJoinPoint proceedingJoinPoint) throws Throwable { // NOSONAR
        Timer timer = meterRegistry.timer(
                "fasit.repository",
                "class", proceedingJoinPoint.getSignature().getDeclaringTypeName(),
                "method", proceedingJoinPoint.getSignature().getName());
        try {
            return timer.recordCallable(() -> {
                try {
                    return proceedingJoinPoint.proceed(proceedingJoinPoint.getArgs());
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            });
        } catch (Exception e) {
            throw e;
        }
    }

}