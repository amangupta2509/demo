package com.molsys.example2.Repository;

import com.molsys.example2.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByRefreshToken(String token);
    Optional<User> findByPasswordResetToken(String token);
}