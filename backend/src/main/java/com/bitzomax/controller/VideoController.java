package com.bitzomax.controller;

import com.bitzomax.dto.VideoDTO;
import com.bitzomax.dto.VideoUploadDTO;
import com.bitzomax.entity.User;
import com.bitzomax.entity.Video;
import com.bitzomax.security.UserPrincipal;
import com.bitzomax.service.UserService;
import com.bitzomax.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videosPage = videoService.getAllVideos(pageable);
        
        final User user;
        if (currentUser != null) {
            user = userService.findById(currentUser.getId()).orElse(null);
        } else {
            user = null;
        }
        
        List<VideoDTO> videos = videosPage.getContent().stream()
                .map(video -> videoService.convertToDTO(video, user))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("videos", videos);
        response.put("currentPage", videosPage.getNumber());
        response.put("totalItems", videosPage.getTotalElements());
        response.put("totalPages", videosPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> getPublicVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videosPage = videoService.getPremiumVideos(false, pageable);
        
        List<VideoDTO> videos = videosPage.getContent().stream()
                .map(video -> videoService.convertToDTO(video, null))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("videos", videos);
        response.put("currentPage", videosPage.getNumber());
        response.put("totalItems", videosPage.getTotalElements());
        response.put("totalPages", videosPage.getTotalPages());

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/premium")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getPremiumVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isSubscribed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Subscription required to access premium videos"));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videosPage = videoService.getPremiumVideos(true, pageable);
        
        List<VideoDTO> videos = videosPage.getContent().stream()
                .map(video -> videoService.convertToDTO(video, user))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("videos", videos);
        response.put("currentPage", videosPage.getNumber());
        response.put("totalItems", videosPage.getTotalElements());
        response.put("totalPages", videosPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVideoById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        final User user;
        if (currentUser != null) {
            user = userService.findById(currentUser.getId()).orElse(null);
        } else {
            user = null;
        }

        return videoService.getVideoById(id)
                .map(video -> {
                    if (video.isPremium() && (user == null || !user.isSubscribed())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "Subscription required to view this video"));
                    }
                    videoService.incrementViewCount(video);
                    return ResponseEntity.ok(videoService.convertToDTO(video, user));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadVideo(
            @RequestPart("videoData") VideoUploadDTO videoData,
            @RequestPart("videoFile") MultipartFile videoFile,
            @RequestPart("coverImage") MultipartFile coverImage,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        try {
            User user = userService.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Video video = videoService.uploadVideo(videoData, videoFile, coverImage, user);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(videoService.convertToDTO(video, user));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload video: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteVideo(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        try {
            User user = userService.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            videoService.deleteVideo(id, user);
            return ResponseEntity.ok(Map.of("message", "Video deleted successfully"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete video: " + e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateVideo(
            @PathVariable Long id,
            @RequestBody VideoUploadDTO videoData,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        try {
            User user = userService.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Video video = videoService.updateVideoDetails(id, videoData, user);
            return ResponseEntity.ok(videoService.convertToDTO(video, user));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamVideo(@PathVariable Long id, 
                                               @RequestHeader(value = "Range", required = false) String rangeHeader,
                                               @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Video video = videoService.getVideoById(id)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            // Check if premium and user has subscription
            final boolean canStreamFull;
            if (video.isPremium()) {
                final User user;
                if (currentUser != null) {
                    user = userService.findById(currentUser.getId()).orElse(null);
                } else {
                    user = null;
                }
                
                canStreamFull = !(user == null || !user.isSubscribed());
            } else {
                canStreamFull = true;
            }

            Path videoPath = Paths.get(video.getVideoPath());
            Resource videoResource = new UrlResource(videoPath.toUri());

            if (!videoResource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Set headers for video streaming
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("video/mp4")); // Adjust content type as needed

            // For free users, limit streaming to 30 seconds
            if (!canStreamFull) {
                // This would be handled on the frontend instead of here
                // Since we can't actually limit the content, we'll just send the headers
                headers.add("X-Premium-Required", "true");
                headers.add("X-Preview-Duration", "30"); // 30 seconds preview
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(videoResource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getThumbnail(@PathVariable Long id) {
        try {
            Video video = videoService.getVideoById(id)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            Path thumbnailPath = Paths.get(video.getCoverImagePath());
            Resource thumbnailResource = new UrlResource(thumbnailPath.toUri());

            if (!thumbnailResource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG) // Adjust content type as needed
                    .body(thumbnailResource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> likeVideo(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return videoService.getVideoById(id)
                .map(video -> {
                    boolean isLiked = videoService.toggleLike(video, user);
                    return ResponseEntity.ok(Map.of(
                            "liked", isLiked,
                            "likeCount", video.getLikeCount()
                    ));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/favorite")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> favoriteVideo(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return videoService.getVideoById(id)
                .map(video -> {
                    boolean isFavorited = videoService.toggleFavorite(video, user);
                    return ResponseEntity.ok(Map.of("favorited", isFavorited));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchVideos(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videosPage = videoService.searchVideos(query, pageable);

        final User user;
        if (currentUser != null) {
            user = userService.findById(currentUser.getId()).orElse(null);
        } else {
            user = null;
        }

        List<VideoDTO> videos = videosPage.getContent().stream()
                .filter(video -> !video.isPremium() || (user != null && user.isSubscribed()))
                .map(video -> videoService.convertToDTO(video, user))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("videos", videos);
        response.put("currentPage", videosPage.getNumber());
        response.put("totalItems", videosPage.getTotalElements());
        response.put("totalPages", videosPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videosPage = videoService.getMostPopularVideos(pageable);

        final User user;
        if (currentUser != null) {
            user = userService.findById(currentUser.getId()).orElse(null);
        } else {
            user = null;
        }

        List<VideoDTO> videos = videosPage.getContent().stream()
                .filter(video -> !video.isPremium() || (user != null && user.isSubscribed()))
                .map(video -> videoService.convertToDTO(video, user))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("videos", videos);
        response.put("currentPage", videosPage.getNumber());
        response.put("totalItems", videosPage.getTotalElements());
        response.put("totalPages", videosPage.getTotalPages());

        return ResponseEntity.ok(response);
    }
}