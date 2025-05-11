package com.molsys.example2.controller;
import com.molsys.example2.Entity.Comment;
import com.molsys.example2.Entity.User;
import com.molsys.example2.Repository.UserRepository;
import com.molsys.example2.dto.CommentResponse;
import com.molsys.example2.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final UserRepository userRepository;
    @GetMapping("/post/{postId}")
    public List<CommentResponse> getCommentsForPost(@PathVariable Long postId) {
        return commentService.getCommentsByPostId(postId).stream()
                .map(comment -> new CommentResponse(
                        comment.getId(),
                        comment.getMessage(),
                        comment.getPost() != null ? comment.getPost().getId() : null,
                        comment.getUser() != null ? comment.getUser().getId() : null
                ))
                .collect(Collectors.toList());
    }
    @PostMapping
    public CommentResponse addComment(@RequestBody Comment comment, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        comment.setUser(user);
        Comment saved = commentService.addComment(comment);

        return new CommentResponse(
                saved.getId(),
                saved.getMessage(),
                saved.getPost() != null ? saved.getPost().getId() : null,
                user.getId()
        );
    }
}