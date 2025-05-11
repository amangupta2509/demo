package com.molsys.example2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String message;
    private Long postId;
    private Long userId;
}
