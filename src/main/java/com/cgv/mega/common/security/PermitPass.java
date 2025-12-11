package com.cgv.mega.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

public record PermitPass(
        HttpMethod httpMethod,
        String pathPattern
) {
    private static final AntPathMatcher matcher = new AntPathMatcher();

    public boolean matches(HttpServletRequest request) {
        String reqMethod = request.getMethod();
        String reqPath   = request.getRequestURI();

        return (httpMethod == null || httpMethod.name().equalsIgnoreCase(reqMethod))
                && matcher.match(pathPattern, reqPath);
    }
}
