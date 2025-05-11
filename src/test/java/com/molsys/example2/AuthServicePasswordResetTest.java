package com.molsys.example2.service;

import com.molsys.example2.Entity.User;
import com.molsys.example2.Repository.UserRepository;
import com.molsys.example2.dto.ResetPasswordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServicePasswordResetTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded_password");
    }

    @Test
    void initiatePasswordReset_ShouldGenerateTokenAndSendEmail() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        authService.initiatePasswordReset("test@example.com");

        // Assert
        verify(userRepository).save(any(User.class));
        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), any(String.class));

        // Capture the saved user to verify token was set
        verify(userRepository).save(argThat(user ->
                user.getPasswordResetToken() != null &&
                        user.getPasswordResetTokenExpiry() != null
        ));
    }

    @Test
    void resetPassword_WithValidTokenAndMatchingPasswords_ShouldUpdatePassword() {
        // Arrange
        String validToken = "valid_token";
        testUser.setPasswordResetToken(validToken);
        testUser.setPasswordResetTokenExpiry(Instant.now().plusSeconds(900)); // 15 minutes from now

        when(userRepository.findByPasswordResetToken(validToken)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("new_encoded_password");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(validToken);
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        // Act
        authService.resetPassword(request);

        // Assert
        verify(userRepository).save(argThat(user ->
                user.getPassword().equals("new_encoded_password") &&
                        user.getPasswordResetToken() == null &&
                        user.getPasswordResetTokenExpiry() == null &&
                        user.getRefreshToken() == null &&
                        user.getRefreshTokenExpiry() == null
        ));
    }

    @Test
    void resetPassword_WithExpiredToken_ShouldThrowException() {
        // Arrange
        String expiredToken = "expired_token";
        testUser.setPasswordResetToken(expiredToken);
        testUser.setPasswordResetTokenExpiry(Instant.now().minusSeconds(60)); // 1 minute ago

        when(userRepository.findByPasswordResetToken(expiredToken)).thenReturn(Optional.of(testUser));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(expiredToken);
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.resetPassword(request);
        });

        assertEquals("Password reset token has expired", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_WithNonMatchingPasswords_ShouldThrowException() {
        // Arrange
        String validToken = "valid_token";
        testUser.setPasswordResetToken(validToken);
        testUser.setPasswordResetTokenExpiry(Instant.now().plusSeconds(900));

        when(userRepository.findByPasswordResetToken(validToken)).thenReturn(Optional.of(testUser));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(validToken);
        request.setNewPassword("newPassword");
        request.setConfirmPassword("differentPassword");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.resetPassword(request);
        });

        assertEquals("Passwords do not match", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}