package com.molsys.example2.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role = Role.ADMIN;

    private String refreshToken;

    private Instant refreshTokenExpiry;

    // Password reset fields
    private String passwordResetToken;

    private Instant passwordResetTokenExpiry;
}