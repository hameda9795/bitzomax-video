package com.bitzomax.controller.admin;

import com.bitzomax.dto.PagedResponse;
import com.bitzomax.dto.VideoDTO;
import com.bitzomax.dto.VideoRequest;
import com.bitzomax.entity.User;
import com.bitzomax.entity.Video;
import com.bitzomax.security.CurrentUser;
import com.bitzomax.security.UserPrincipal;
import com.bitzomax.service.VideoService;
import com.bitzomax.service.UserService;
import com.bitzomax.util.WebmConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/videos")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebmConverter webmConverter;

    @GetMapping
    public ResponseEntity<PagedResponse<VideoDTO>> getVideos(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        PagedResponse<VideoDTO> response = videoService.getAllVideosAdmin(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoDTO> getVideo(@PathVariable Long id) {
        VideoDTO videoDTO = videoService.getVideoDTOById(id);
        return ResponseEntity.ok(videoDTO);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addVideo(
            @RequestPart("video") @Valid VideoRequest videoRequest,
            @RequestPart("videoFile") MultipartFile videoFile,
            @RequestPart("thumbnail") MultipartFile thumbnail,
            @RequestPart(value = "convertToWebm", required = false) String convertToWebm,
            @CurrentUser UserPrincipal currentUser) {
        
        try {
            User user = userService.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Convert to WebM if requested (backend conversion)
            if (convertToWebm != null && convertToWebm.equalsIgnoreCase("true") 
                    && !videoFile.getContentType().contains("webm")) {
                
                byte[] webmData = webmConverter.convertToWebm(videoFile.getBytes());
                VideoDTO videoDTO = videoService.createVideo(videoRequest, webmData, thumbnail.getBytes(), user);
                return ResponseEntity.ok(videoDTO);
            } else {
                // Use original format
                VideoDTO videoDTO = videoService.createVideo(videoRequest, videoFile.getBytes(), thumbnail.getBytes(), user);
                return ResponseEntity.ok(videoDTO);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process video: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVideo(
            @PathVariable Long id,
            @RequestBody @Valid VideoRequest videoRequest,
            @CurrentUser UserPrincipal currentUser) {
        
        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        VideoDTO videoDTO = videoService.updateVideo(id, videoRequest, user);
        return ResponseEntity.ok(videoDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long id) {
        videoService.deleteVideo(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Video deleted successfully");
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}/premium")
    public ResponseEntity<?> updatePremiumStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload) {
        
        Boolean premium = payload.get("premium");
        if (premium == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Premium status not specified"));
        }
        
        Optional<Video> videoOpt = videoService.findById(id);
        if (videoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Video video = videoOpt.get();
        video.setPremium(premium);
        Video savedVideo = videoService.save(video);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "video", videoService.convertToDTO(savedVideo, null)
        ));
    }

    @PostMapping("/{id}/thumbnail")
    public ResponseEntity<?> updateThumbnail(
            @PathVariable Long id,
            @RequestPart("thumbnail") MultipartFile thumbnail) {
        
        try {
            VideoDTO videoDTO = videoService.updateThumbnail(id, thumbnail.getBytes());
            return ResponseEntity.ok(videoDTO);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update thumbnail: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/convert-webm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> convertToWebm(@RequestPart("video") MultipartFile videoFile) {
        try {
            if (videoFile.getContentType().contains("webm")) {
                return ResponseEntity.ok(Map.of(
                        "message", "File is already in WebM format",
                        "originalSize", videoFile.getSize()
                ));
            }
            
            byte[] webmData = webmConverter.convertToWebm(videoFile.getBytes());
            
            return ResponseEntity.ok(Map.of(
                    "message", "Conversion successful",
                    "originalSize", videoFile.getSize(),
                    "convertedSize", webmData.length,
                    "reductionPercent", 100 - ((webmData.length * 100) / videoFile.getSize())
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to convert video: " + e.getMessage()));
        }
    }
}