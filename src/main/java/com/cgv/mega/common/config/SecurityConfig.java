package com.cgv.mega.common.config;

import com.cgv.mega.common.logging.RequestTraceFilter;
import com.cgv.mega.common.security.JwtAuthenticationFilter;
import com.cgv.mega.common.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(redisTemplate, jwtTokenProvider);
    }

    @Bean
    public RequestTraceFilter requestTraceFilter() {
        return new RequestTraceFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter,
                                           RequestTraceFilter requestTraceFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // 토큰 관련 API
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()

                        // 영화 상세 정보 조회 API
                        .requestMatchers(HttpMethod.GET, "/api/movies/**").permitAll()

                        // 상영관 목록 API
                        .requestMatchers(HttpMethod.GET, "/api/theaters").permitAll()

                        // 영화 상영 목록, 상영중인 영화 목록, 상영회차별 좌석 현황 API
                        .requestMatchers(HttpMethod.GET,
                                "/api/screenings/movies",
                                "/api/screenings/*/seats",
                                "/api/screenings/*").permitAll()

                        // ADMIN 전용 API
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 회원가입 API
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                        .anyRequest().authenticated()
                )
                // 인증 실패 시 401 반환
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                        })
                )
                .addFilterBefore(requestTraceFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, RequestTraceFilter.class)
                .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}
