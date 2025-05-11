package com.molsys.example2.Repository;

import com.molsys.example2.Entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface PostRepository extends JpaRepository<Post, Long> {
    // Search by title containing a keyword (case-insensitive)
    List<Post> findByTitleContainingIgnoreCase(String keyword);

}