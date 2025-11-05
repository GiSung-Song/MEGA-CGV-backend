package com.cgv.mega.util;

import com.cgv.mega.common.enums.Role;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = CustomMockUserFactory.class)
public @interface CustomMockUser {

    long id();
    String name();
    String email();
    Role role();
}
