package com.bitzomax.dto;

import com.bitzomax.entity.Video;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoDTO {
    private Long id;
    private String title;
    private String description;
    private String poemText;
    private String videoUrl;
    private String coverImageUrl;
    private long duration;
    private boolean premium;
    private long viewCount;
    private long likeCount;
    private boolean likedByCurrentUser;
    private boolean favoriteByCurrentUser;
    private UserSummaryDTO uploader;
    private Set<String> tags;
    private String seoTitle;
    private String seoDescription;
    private String seoKeywords;
    private LocalDateTime createdAt;
    
    // Static method to convert Video entity to VideoDTO
    public static VideoDTO fromVideo(Video video) {
        VideoDTO dto = new VideoDTO();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setPoemText(video.getPoemText());
        dto.setVideoUrl(video.getVideoPath()); // Changed from getVideoUrl() to getVideoPath()
        dto.setCoverImageUrl(video.getCoverImagePath()); // Changed from getCoverImageUrl() to getCoverImagePath()
        dto.setDuration(video.getDuration());
        dto.setPremium(video.isPremium());
        dto.setViewCount(video.getViewCount());
        dto.setLikeCount(video.getLikeCount());
        
        // These fields might need to be set separately based on the current user context
        dto.setLikedByCurrentUser(false);
        dto.setFavoriteByCurrentUser(false);
        
        // If the video entity has tags, map them to strings
        if (video.getTags() != null) {
            dto.setTags(video.getTags().stream()
                .map(tag -> tag.getName())
                .collect(Collectors.toSet()));
        }
        
        dto.setSeoTitle(video.getSeoTitle());
        dto.setSeoDescription(video.getSeoDescription());
        dto.setSeoKeywords(video.getSeoKeywords());
        dto.setCreatedAt(video.getCreatedAt());
        
        // Set uploader if available
        if (video.getUploader() != null) {
            UserSummaryDTO userSummary = new UserSummaryDTO();
            userSummary.setId(video.getUploader().getId());
            userSummary.setUsername(video.getUploader().getUsername());
            userSummary.setFullName(video.getUploader().getFullName()); // Changed from setName to setFullName
            userSummary.setProfilePicture(video.getUploader().getProfilePicture()); // Changed from setProfileImageUrl to setProfilePicture
            dto.setUploader(userSummary);
        }
        
        return dto;
    }
}