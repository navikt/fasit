package no.nav.aura.envconfig.spring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

@Aspect
public class PerformanceMeasureAspect {

    @Around(value = "execution(* no.nav.aura.envconfig.FasitRepository.*(..))")
    public Object checkIntegrity(ProceedingJoinPoint proceedingJoinPoint) throws Throwable { // NOSONAR
        Monitor monitor = MonitorFactory.start(proceedingJoinPoint.getSignature().getDeclaringTypeName() + "#" + proceedingJoinPoint.getSignature().getName());
        try {
            return proceedingJoinPoint.proceed(proceedingJoinPoint.getArgs());
        } finally {
            monitor.stop();
        }
    }

}
