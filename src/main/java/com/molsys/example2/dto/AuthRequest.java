package com.molsys.example2.dto;

import com.molsys.example2.Entity.Role;
import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String password;
    private Role role;  // Added role field
}