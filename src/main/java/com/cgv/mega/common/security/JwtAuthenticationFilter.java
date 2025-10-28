package com.cgv.mega.common.security;

import com.cgv.mega.auth.dto.JwtPayloadDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    private final List<PermitPass> PASS_PATHS = List.of(
            new PermitPass(HttpMethod.POST, "/api/auth/login"),
            new PermitPass(HttpMethod.POST, "/api/auth/refresh"),
            new PermitPass(HttpMethod.POST, "/api/users")
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestHeader = request.getHeader("Authorization");

        boolean isPermitPass = PASS_PATHS.stream().anyMatch(p -> p.matches(request));
        boolean hasToken = StringUtils.hasText(requestHeader) && requestHeader.startsWith("Bearer ");

        if (hasToken) {
            String accessToken = requestHeader.substring(7);

            if (!jwtTokenProvider.validateToken(accessToken) || isLogout(accessToken)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            JwtPayloadDto jwtPayload = jwtTokenProvider.parseToken(accessToken);

            CustomUserDetails customUserDetails = new CustomUserDetails(
                    jwtPayload.userId(),
                    jwtPayload.name(),
                    jwtPayload.email(),
                    jwtPayload.role()
            );

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            customUserDetails,
                            null,
                            customUserDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        } else if (!isPermitPass) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLogout(String accessToken) {
        return jwtTokenProvider.tokenToHash(accessToken)
                .map(hash -> redisTemplate.opsForValue().get(hash) != null)
                .orElse(false);
    }
}