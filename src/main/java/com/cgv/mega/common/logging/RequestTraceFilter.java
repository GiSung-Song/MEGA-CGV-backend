package com.cgv.mega.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class RequestTraceFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_UUID_KEY = "requestId";
    private static final String MDC_IP_KEY = "clientIp";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || !StringUtils.hasText(requestId)) {
                requestId = UUID.randomUUID().toString();
            }

            MDC.put(MDC_UUID_KEY, requestId);
            response.addHeader(REQUEST_ID_HEADER, requestId);

            String clientIp = request.getHeader("X-Forwarded-For");
            if (!StringUtils.hasText(clientIp)) {
                clientIp = request.getRemoteAddr();
            }
            MDC.put(MDC_IP_KEY, clientIp != null ? clientIp : "UNKNOWN");

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_UUID_KEY);
            MDC.remove(MDC_IP_KEY);
        }
    }
}
