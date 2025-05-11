package com.molsys.example2.service;

import com.molsys.example2.Entity.Role;
import com.molsys.example2.Entity.User;
import com.molsys.example2.Repository.UserRepository;
import com.molsys.example2.dto.*;
import com.molsys.example2.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j

public class AuthService {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    // Token expiry time - 15 minutes
    private static final long PASSWORD_RESET_TOKEN_EXPIRY = 15 * 60 * 1000;

    public AuthResponse register(AuthRequest request) {
        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        // Use the role from request if provided, otherwise default to USER
        Role userRole = (request.getRole() != null) ? request.getRole() : Role.USER;

        // Save user first to get a generated ID
        User user = User.builder()
                .email(request.getEmail())
                .name("User")
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole) // Use the role from request or default
                .build();

        user = userRepo.save(user); // Save to assign ID

        // Now generate tokens using saved user
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(Instant.now().plusMillis(7 * 24 * 60 * 60 * 1000));
        userRepo.save(user); // Save refresh token and expiry

        return new AuthResponse(accessToken, refreshToken, user.getRole().name());
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(Instant.now().plusMillis(7 * 24 * 60 * 60 * 1000));
        userRepo.save(user);

        return new AuthResponse(accessToken, refreshToken, user.getRole().name());
    }

    public AuthResponse refreshToken(String token) {
        var claims = jwtService.validateRefreshToken(token).getBody();
        Long userId = claims.get("id", Integer.class).longValue();

        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!token.equals(user.getRefreshToken())) {
            throw new RuntimeException("Invalid refresh token");
        }

        String newAccess = jwtService.generateAccessToken(user.getId(), user.getRole());
        return new AuthResponse(newAccess, token, user.getRole().name());
    }

    public void logout(String refreshToken) {
        Optional<User> user = userRepo.findByRefreshToken(refreshToken);
        user.ifPresent(u -> {
            u.setRefreshToken(null);
            u.setRefreshTokenExpiry(null);
            userRepo.save(u);
        });
    }

    public void changePassword(String email, PasswordChangeRequest request) {
        // Validate request
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("New password cannot be empty");
        }

        if (!Objects.equals(request.getNewPassword(), request.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        // Find user
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Check if new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // Invalidate refresh tokens for security
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);

        // Save changes
        userRepo.save(user);
    }

    // Password Reset Methods
    @Transactional
    public void initiatePasswordReset(String email) {
        Optional<User> userOptional = userRepo.findByEmail(email);

        // If user exists, generate and send token
        userOptional.ifPresent(user -> {
            try {
                String token = generatePasswordResetToken();
                user.setPasswordResetToken(token);
                user.setPasswordResetTokenExpiry(Instant.now().plusMillis(PASSWORD_RESET_TOKEN_EXPIRY));
                userRepo.save(user);

                // Send email with reset link
                emailService.sendPasswordResetEmail(user.getEmail(), token);
                log.info("Password reset initiated for user: {}", email);
            } catch (Exception e) {
                log.error("Error initiating password reset for user: {}", email, e);
                throw new RuntimeException("Failed to initiate password reset", e);
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Reset token cannot be empty");
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("New password cannot be empty");
        }

        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = userRepo.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        // Check token expiry
        if (user.getPasswordResetTokenExpiry() == null ||
                user.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            throw new RuntimeException("Password reset token has expired");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // Clear reset token and invalidate any existing sessions for security
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);

        userRepo.save(user);
        log.info("Password reset completed for user: {}", user.getEmail());
    }

    private String generatePasswordResetToken() {
        return UUID.randomUUID().toString();
    }
}