package com.bitzomax.repository;

import com.bitzomax.entity.User;
import com.bitzomax.entity.Video;
import com.bitzomax.entity.VideoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoLikeRepository extends JpaRepository<VideoLike, Long> {
    Optional<VideoLike> findByUserAndVideo(User user, Video video);
    
    boolean existsByUserAndVideo(User user, Video video);
    
    List<VideoLike> findByVideo(Video video);
    
    List<VideoLike> findByUser(User user);
    
    long countByVideo(Video video);
    
    @Query("SELECT COUNT(vl) FROM VideoLike vl WHERE vl.video.id = :videoId")
    long countByVideoId(@Param("videoId") Long videoId);
    
    void deleteByUserAndVideo(User user, Video video);
}