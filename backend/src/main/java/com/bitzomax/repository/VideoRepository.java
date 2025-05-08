package com.bitzomax.repository;

import com.bitzomax.entity.Tag;
import com.bitzomax.entity.User;
import com.bitzomax.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    Page<Video> findAll(Pageable pageable);
    
    Page<Video> findByUploader(User uploader, Pageable pageable);
    
    Page<Video> findByTagsIn(List<Tag> tags, Pageable pageable);
    
    @Query("SELECT v FROM Video v WHERE v.title LIKE %:query% OR v.description LIKE %:query% OR v.poemText LIKE %:query%")
    Page<Video> searchVideos(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT v FROM Video v JOIN v.tags t WHERE t.name IN :tagNames")
    Page<Video> findByTagNames(@Param("tagNames") List<String> tagNames, Pageable pageable);
    
    // Find most popular videos based on likes or views
    Page<Video> findAllByOrderByLikeCountDesc(Pageable pageable);
    
    Page<Video> findAllByOrderByViewCountDesc(Pageable pageable);
    
    // Find premium or free videos
    Page<Video> findByPremium(boolean isPremium, Pageable pageable);
    
    // Find videos favorited by a user
    @Query("SELECT v FROM Video v JOIN v.favoredBy u WHERE u.id = :userId")
    Page<Video> findFavoritesByUserId(@Param("userId") Long userId, Pageable pageable);
    
    // Count videos by premium flag
    long countByPremiumTrue();
}