package com.molsys.example2.controller;

import com.molsys.example2.Entity.Post;
import com.molsys.example2.Entity.User;
import com.molsys.example2.Repository.UserRepository;
import com.molsys.example2.dto.PostResponse;
import com.molsys.example2.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserRepository userRepository;

    @GetMapping
    public List<PostResponse> getAllPosts() {
        return postService.getAllPosts().stream()
                .map(post -> new PostResponse(
                        post.getId(),
                        post.getTitle(),
                        post.getContent(),
                        post.getUser() != null ? post.getUser().getId() : null
                ))
                .collect(Collectors.toList());
    }

    @PostMapping
    public PostResponse createPost(@RequestBody Post post, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        post.setUser(user);
        Post savedPost = postService.createPost(post);

        return new PostResponse(
                savedPost.getId(),
                savedPost.getTitle(),
                savedPost.getContent(),
                user.getId()
        );
    }

    @GetMapping("/search")
    public List<PostResponse> searchPosts(@RequestParam("keyword") String keyword) {
        return postService.searchPosts(keyword).stream()
                .map(post -> new PostResponse(
                        post.getId(),
                        post.getTitle(),
                        post.getContent(),
                        post.getUser() != null ? post.getUser().getId() : null
                ))
                .collect(Collectors.toList());
    }
}
