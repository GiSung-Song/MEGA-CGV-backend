package com.cgv.mega.util;

import com.cgv.mega.auth.dto.JwtPayloadDto;
import com.cgv.mega.common.enums.Role;
import com.cgv.mega.common.security.JwtTokenProvider;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.user.entity.User;
import com.cgv.mega.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("test")
@Transactional
public class TestDataFactory {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public User createUser(String name, String email, String phoneNumber) {
        User user = User.createUser(name, email, passwordEncoder.encode("rawPassword"), phoneNumber);

        return userRepository.save(user);
    }

    public Movie createMovie(String title) {
        Movie movie = Movie.createMovie(title, 150, "test-description", "poster.png");

        return movieRepository.save(movie);
    }

    public void setAdmin(User user) {
        ReflectionTestUtils.setField(user, "role", Role.ADMIN);
    }

    public String setLogin(User user) {
        JwtPayloadDto jwtPayloadDto = new JwtPayloadDto(user.getId(), user.getEmail(), user.getName(), user.getRole());

        return "Bearer " + jwtTokenProvider.generateAccessToken(jwtPayloadDto);
    }
}
