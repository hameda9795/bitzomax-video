package com.bitzomax.repository;

import com.bitzomax.entity.VideoView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface VideoViewRepository extends JpaRepository<VideoView, Long> {
    long countByViewedAtBetween(LocalDateTime start, LocalDateTime end);
}