package com.bitzomax.repository;

import com.bitzomax.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    
    boolean existsByName(String name);
    
    @Query("SELECT t FROM Tag t WHERE t.name IN :tagNames")
    List<Tag> findByTagNames(List<String> tagNames);
    
    @Query(value = "SELECT t.* FROM tags t JOIN video_tags vt ON t.id = vt.tag_id GROUP BY t.id ORDER BY COUNT(vt.video_id) DESC LIMIT :limit", nativeQuery = true)
    List<Tag> findMostUsedTags(int limit);
}