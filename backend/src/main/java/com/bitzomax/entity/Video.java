package com.bitzomax.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "videos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT", name = "poem_text")
    private String poemText;

    private String videoPath;
    
    @Column(name = "cover_image_path")
    private String coverImagePath;
    
    private long duration; // in seconds
    
    @Column(name = "is_premium")
    private boolean premium = false;
    
    @Column(name = "view_count")
    private long viewCount = 0;
    
    @Column(name = "like_count")
    private long likeCount = 0;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User uploader;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "video_tags",
        joinColumns = @JoinColumn(name = "video_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany(mappedBy = "favoriteVideos")
    private Set<User> favoredBy = new HashSet<>();
    
    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VideoLike> likes = new HashSet<>();
    
    // SEO fields
    @Column(name = "seo_title")
    private String seoTitle;
    
    @Column(name = "seo_description", columnDefinition = "TEXT")
    private String seoDescription;
    
    @Column(name = "seo_keywords")
    private String seoKeywords;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}