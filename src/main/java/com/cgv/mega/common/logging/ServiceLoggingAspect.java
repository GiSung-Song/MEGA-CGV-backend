package com.cgv.mega.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    @Pointcut("execution(* com.cgv.mega..service.*.*(..))")
    public void serviceMethods() {}

    @Around("serviceMethods()")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        String method = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("[START] {} args = {}", method, args);

        try {
            Object result = joinPoint.proceed();
            long time = System.currentTimeMillis() - start;

            log.info("[END] {} return = {} ({}ms)", method, result, time);
            return result;
        } catch (Exception e) {
            long time = System.currentTimeMillis() - start;

            log.error("[EXCEPTION] {} args = {} ({}ms) ex = {}",
                    method, args, time, e.getMessage(), e);

            throw e;
        }
    }
}
