package com.cgv.mega.util;

import com.cgv.mega.common.enums.Role;
import com.cgv.mega.common.security.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class CustomMockUserFactory implements WithSecurityContextFactory<CustomMockUser> {

    @Override
    public SecurityContext createSecurityContext(CustomMockUser annotation) {
        Long userId = annotation.id();
        String name = annotation.name();
        String email = annotation.email();
        Role role = annotation.role();

        CustomUserDetails customUserDetails = new CustomUserDetails(userId, email, name, role);

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                customUserDetails, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(token);

        return context;
    }
}
