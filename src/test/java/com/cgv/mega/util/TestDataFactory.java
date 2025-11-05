package com.cgv.mega.util;

import com.cgv.mega.user.entity.User;
import com.cgv.mega.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestDataFactory {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User createUser(String name, String email, String phoneNumber) {
        User user = User.createUser(name, email, passwordEncoder.encode("rawPassword"), phoneNumber);

        return userRepository.save(user);
    }
}
