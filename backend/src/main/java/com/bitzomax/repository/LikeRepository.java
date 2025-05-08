package com.bitzomax.repository;

import com.bitzomax.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}