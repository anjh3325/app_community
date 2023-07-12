package com.company.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.company.model.entity.Post;

public interface PostRepository extends JpaRepository<Post, Integer> {
	


}
