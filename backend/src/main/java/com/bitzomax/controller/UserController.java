package com.bitzomax.controller;

import com.bitzomax.dto.UserSummaryDTO;
import com.bitzomax.dto.VideoDTO;
import com.bitzomax.entity.User;
import com.bitzomax.entity.Video;
import com.bitzomax.security.UserPrincipal;
import com.bitzomax.service.UserService;
import com.bitzomax.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private VideoService videoService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No authenticated user found");
        }

        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("profilePicture", user.getProfilePicture());
        response.put("bio", user.getBio());
        response.put("role", user.getRole().name());
        response.put("subscribed", user.isSubscribed());
        
        if (user.isSubscribed()) {
            response.put("subscriptionStartDate", user.getSubscriptionStartDate());
            response.put("subscriptionEndDate", user.getSubscriptionEndDate());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getUserProfile(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(user -> {
                    UserSummaryDTO userSummary = userService.convertToSummaryDTO(user);
                    return ResponseEntity.ok(userSummary);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{username}/videos")
    public ResponseEntity<Map<String, Object>> getUserVideos(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        return userService.findByUsername(username)
                .map(user -> {
                    Pageable pageable = PageRequest.of(page, size);
                    Page<Video> videosPage = videoService.getVideosByUploader(user, pageable);
                    
                    // Make currentUserEntity final to use it in lambda
                    final User currentUserEntity;
                    if (currentUser != null) {
                        currentUserEntity = userService.findById(currentUser.getId()).orElse(null);
                    } else {
                        currentUserEntity = null;
                    }
                    
                    List<VideoDTO> videos = videosPage.getContent().stream()
                            .filter(video -> !video.isPremium() || (currentUserEntity != null && currentUserEntity.isSubscribed()))
                            .map(video -> videoService.convertToDTO(video, currentUserEntity))
                            .collect(Collectors.toList());

                    Map<String, Object> response = new HashMap<>();
                    response.put("videos", videos);
                    response.put("currentPage", videosPage.getNumber());
                    response.put("totalItems", videosPage.getTotalElements());
                    response.put("totalPages", videosPage.getTotalPages());
                    
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/favorites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getFavoriteVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videosPage = videoService.getFavoriteVideosByUserId(user.getId(), pageable);
        
        List<VideoDTO> videos = videosPage.getContent().stream()
                .filter(video -> !video.isPremium() || user.isSubscribed())
                .map(video -> videoService.convertToDTO(video, user))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("videos", videos);
        response.put("currentPage", videosPage.getNumber());
        response.put("totalItems", videosPage.getTotalElements());
        response.put("totalPages", videosPage.getTotalPages());
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUserProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody Map<String, String> profileUpdate) {

        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (profileUpdate.containsKey("fullName")) {
            user.setFullName(profileUpdate.get("fullName"));
        }

        if (profileUpdate.containsKey("bio")) {
            user.setBio(profileUpdate.get("bio"));
        }

        // Update other fields as needed
        
        User updatedUser = userService.updateUser(user);
        return ResponseEntity.ok(userService.convertToSummaryDTO(updatedUser));
    }
}