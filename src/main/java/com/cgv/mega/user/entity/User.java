package com.cgv.mega.user.entity;

import com.cgv.mega.common.entity.BaseTimeEntity;
import com.cgv.mega.common.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {@UniqueConstraint(name = "uq_users_email", columnNames = {"email"}),
                @UniqueConstraint(name = "uq_users_phone_number", columnNames = {"phone_number"})}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50, updatable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20, name = "phone_number")
    private String phoneNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder(access = AccessLevel.PRIVATE)
    private User(String name, String email, String password, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.role = Role.USER;
    }

    public static User createUser(String name, String email, String password, String phoneNumber) {
        return User.builder()
                .name(name)
                .email(email)
                .password(password)
                .phoneNumber(phoneNumber)
                .build();
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void changePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
