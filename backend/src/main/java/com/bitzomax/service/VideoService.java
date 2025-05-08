package com.bitzomax.service;

import com.bitzomax.dto.VideoDTO;
import com.bitzomax.dto.VideoRequest;
import com.bitzomax.dto.VideoUploadDTO;
import com.bitzomax.dto.PagedResponse;
import com.bitzomax.entity.Tag;
import com.bitzomax.entity.User;
import com.bitzomax.entity.Video;
import com.bitzomax.entity.VideoLike;
import com.bitzomax.repository.VideoLikeRepository;
import com.bitzomax.repository.VideoRepository;
import com.bitzomax.util.FileUtils;
import jakarta.persistence.EntityNotFoundException;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VideoService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private VideoLikeRepository videoLikeRepository;

    @Autowired
    private TagService tagService;

    @Autowired
    private UserService userService;

    @Autowired
    private FileUtils fileUtils;

    @Transactional(readOnly = true)
    public Page<Video> getAllVideos(Pageable pageable) {
        return videoRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Video> getVideoById(Long id) {
        return videoRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Video> getVideosByUploader(User uploader, Pageable pageable) {
        return videoRepository.findByUploader(uploader, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Video> searchVideos(String query, Pageable pageable) {
        return videoRepository.searchVideos(query, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Video> getVideosByTags(List<String> tagNames, Pageable pageable) {
        return videoRepository.findByTagNames(tagNames, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Video> getMostPopularVideos(Pageable pageable) {
        return videoRepository.findAllByOrderByLikeCountDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Video> getMostViewedVideos(Pageable pageable) {
        return videoRepository.findAllByOrderByViewCountDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Video> getPremiumVideos(boolean isPremium, Pageable pageable) {
        return videoRepository.findByPremium(isPremium, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Video> getFavoriteVideosByUserId(Long userId, Pageable pageable) {
        return videoRepository.findFavoritesByUserId(userId, pageable);
    }

    @Transactional
    public Video uploadVideo(VideoUploadDTO videoUploadDTO, MultipartFile videoFile, 
                            MultipartFile coverImage, User uploader) throws IOException {
        // Create directories if they don't exist
        createUploadDirectories();

        // Save video file
        String videoFileName = fileUtils.generateFileName(videoFile.getOriginalFilename());
        String videoPath = saveFile(videoFile, videoFileName);

        // Save cover image
        String coverImageName = fileUtils.generateFileName(coverImage.getOriginalFilename());
        String coverImagePath = saveFile(coverImage, coverImageName);

        // Get video duration
        long duration = getVideoDuration(videoPath);

        // Process tags
        Set<Tag> tags = tagService.getOrCreateTags(videoUploadDTO.getTags());

        // Create Video entity
        Video video = new Video();
        video.setTitle(videoUploadDTO.getTitle());
        video.setDescription(videoUploadDTO.getDescription());
        video.setPoemText(videoUploadDTO.getPoemText());
        video.setVideoPath(videoPath);
        video.setCoverImagePath(coverImagePath);
        video.setDuration(duration);
        video.setPremium(videoUploadDTO.isPremium());
        video.setUploader(uploader);
        video.setTags(tags);
        video.setSeoTitle(videoUploadDTO.getSeoTitle());
        video.setSeoDescription(videoUploadDTO.getSeoDescription());
        video.setSeoKeywords(videoUploadDTO.getSeoKeywords());

        return videoRepository.save(video);
    }

    @Transactional
    public void deleteVideo(Long videoId, User user) throws IOException {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException("Video not found with id: " + videoId));

        // Check if user is the uploader or an admin
        if (video.getUploader().getId().equals(user.getId()) || user.getRole() == User.Role.ADMIN) {
            // Delete video file
            Files.deleteIfExists(Paths.get(video.getVideoPath()));
            
            // Delete cover image
            Files.deleteIfExists(Paths.get(video.getCoverImagePath()));
            
            // Delete video from database
            videoRepository.delete(video);
        } else {
            throw new SecurityException("You don't have permission to delete this video");
        }
    }

    @Transactional
    public Video incrementViewCount(Video video) {
        video.setViewCount(video.getViewCount() + 1);
        return videoRepository.save(video);
    }

    @Transactional
    public boolean toggleLike(Video video, User user) {
        Optional<VideoLike> existingLike = videoLikeRepository.findByUserAndVideo(user, video);
        
        if (existingLike.isPresent()) {
            // Unlike the video
            videoLikeRepository.delete(existingLike.get());
            video.setLikeCount(video.getLikeCount() - 1);
            videoRepository.save(video);
            return false; // Video is now unliked
        } else {
            // Like the video
            VideoLike like = new VideoLike();
            like.setUser(user);
            like.setVideo(video);
            videoLikeRepository.save(like);
            video.setLikeCount(video.getLikeCount() + 1);
            videoRepository.save(video);
            return true; // Video is now liked
        }
    }

    @Transactional
    public boolean toggleFavorite(Video video, User user) {
        if (user.getFavoriteVideos().contains(video)) {
            // Remove from favorites
            user.getFavoriteVideos().remove(video);
            return false; // Video is now unfavorited
        } else {
            // Add to favorites
            user.getFavoriteVideos().add(video);
            return true; // Video is now favorited
        }
    }

    @Transactional
    public Video updateVideoDetails(Long videoId, VideoUploadDTO videoUploadDTO, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException("Video not found with id: " + videoId));

        // Check if user is the uploader or an admin
        if (video.getUploader().getId().equals(user.getId()) || user.getRole() == User.Role.ADMIN) {
            video.setTitle(videoUploadDTO.getTitle());
            video.setDescription(videoUploadDTO.getDescription());
            video.setPoemText(videoUploadDTO.getPoemText());
            video.setPremium(videoUploadDTO.isPremium());
            video.setSeoTitle(videoUploadDTO.getSeoTitle());
            video.setSeoDescription(videoUploadDTO.getSeoDescription());
            video.setSeoKeywords(videoUploadDTO.getSeoKeywords());

            // Update tags
            if (videoUploadDTO.getTags() != null) {
                Set<Tag> tags = tagService.getOrCreateTags(videoUploadDTO.getTags());
                video.setTags(tags);
            }

            return videoRepository.save(video);
        } else {
            throw new SecurityException("You don't have permission to update this video");
        }
    }
    
    public boolean isVideoLikedByUser(Video video, User user) {
        return videoLikeRepository.existsByUserAndVideo(user, video);
    }

    public boolean isVideoFavoritedByUser(Video video, User user) {
        return user.getFavoriteVideos().contains(video);
    }
    
    public VideoDTO convertToDTO(Video video, User currentUser) {
        boolean isLiked = false;
        boolean isFavorite = false;

        if (currentUser != null) {
            isLiked = isVideoLikedByUser(video, currentUser);
            isFavorite = isVideoFavoritedByUser(video, currentUser);
        }

        Set<String> tagNames = video.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        return new VideoDTO(
            video.getId(),
            video.getTitle(),
            video.getDescription(),
            video.getPoemText(),
            "/api/videos/" + video.getId() + "/stream",
            "/api/videos/" + video.getId() + "/thumbnail",
            video.getDuration(),
            video.isPremium(),
            video.getViewCount(),
            video.getLikeCount(),
            isLiked,
            isFavorite,
            userService.convertToSummaryDTO(video.getUploader()),
            tagNames,
            video.getSeoTitle(),
            video.getSeoDescription(),
            video.getSeoKeywords(),
            video.getCreatedAt()
        );
    }

    private String saveFile(MultipartFile file, String fileName) throws IOException {
        Path filePath = Paths.get(uploadDir, fileName);
        Files.copy(file.getInputStream(), filePath);
        return filePath.toString();
    }

    private void createUploadDirectories() throws IOException {
        Path uploadsDir = Paths.get(uploadDir);
        if (!Files.exists(uploadsDir)) {
            Files.createDirectories(uploadsDir);
        }
    }

    private long getVideoDuration(String videoPath) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(new File(videoPath))) {
            grabber.start();
            long durationInMicroseconds = grabber.getLengthInTime();
            return durationInMicroseconds / 1000000; // Convert to seconds
        } catch (Exception e) {
            // Log error but continue (return 0 duration)
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<VideoDTO> getAllVideosAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videos = videoRepository.findAll(pageable);
        
        List<VideoDTO> content = videos.getContent()
            .stream()
            .map(video -> convertToDTO(video, null))
            .collect(Collectors.toList());
            
        return new PagedResponse<>(
            content, 
            videos.getNumber(),
            videos.getSize(),
            videos.getTotalElements(),
            videos.getTotalPages(),
            videos.isLast()
        );
    }
    
    @Transactional
    public VideoDTO getVideoDTOById(Long id) {
        Video video = videoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Video not found with id: " + id));
        return convertToDTO(video, null);
    }

    @Transactional
    public VideoDTO createVideo(VideoRequest videoRequest, byte[] videoData, byte[] thumbnailData, User uploader) throws IOException {
        // Create directories if they don't exist
        createUploadDirectories();

        // Save video file
        String videoFileName = fileUtils.generateFileName("video" + System.currentTimeMillis() + ".mp4");
        Path videoFilePath = Paths.get(uploadDir, videoFileName);
        Files.write(videoFilePath, videoData);

        // Save thumbnail image
        String thumbnailFileName = fileUtils.generateFileName("thumbnail" + System.currentTimeMillis() + ".jpg");
        Path thumbnailFilePath = Paths.get(uploadDir, thumbnailFileName);
        Files.write(thumbnailFilePath, thumbnailData);

        // Get video duration
        long duration = getVideoDuration(videoFilePath.toString());

        // Process tags
        Set<Tag> tags = tagService.getOrCreateTags(videoRequest.getTags());

        // Create Video entity
        Video video = new Video();
        video.setTitle(videoRequest.getTitle());
        video.setDescription(videoRequest.getDescription());
        video.setPoemText(videoRequest.getPoemText());
        video.setVideoPath(videoFilePath.toString());
        video.setCoverImagePath(thumbnailFilePath.toString());
        video.setDuration(duration);
        video.setPremium(videoRequest.getPremium());
        video.setUploader(uploader);
        video.setTags(tags);
        video.setSeoTitle(videoRequest.getSeoTitle());
        video.setSeoDescription(videoRequest.getSeoDescription());
        video.setSeoKeywords(videoRequest.getSeoKeywords());

        video = videoRepository.save(video);
        return convertToDTO(video, null);
    }
    
    @Transactional
    public VideoDTO updateVideo(Long id, VideoRequest videoRequest, User user) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + id));

        // Check if user is admin
        if (user.getRole() != User.Role.ADMIN) {
            throw new SecurityException("You don't have permission to update this video");
        }

        video.setTitle(videoRequest.getTitle());
        video.setDescription(videoRequest.getDescription());
        video.setPoemText(videoRequest.getPoemText());
        video.setPremium(videoRequest.getPremium());
        video.setSeoTitle(videoRequest.getSeoTitle());
        video.setSeoDescription(videoRequest.getSeoDescription());
        video.setSeoKeywords(videoRequest.getSeoKeywords());

        // Update tags
        if (videoRequest.getTags() != null) {
            Set<Tag> tags = tagService.getOrCreateTags(videoRequest.getTags());
            video.setTags(tags);
        }

        video = videoRepository.save(video);
        return convertToDTO(video, null);
    }
    
    @Transactional
    public void deleteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + id));
        
        try {
            // Delete video file
            Files.deleteIfExists(Paths.get(video.getVideoPath()));
            
            // Delete cover image
            Files.deleteIfExists(Paths.get(video.getCoverImagePath()));
        } catch (IOException e) {
            // Log error but continue with database deletion
            System.err.println("Error deleting files: " + e.getMessage());
        }
        
        // Delete video from database
        videoRepository.delete(video);
    }
    
    @Transactional(readOnly = true)
    public Optional<Video> findById(Long id) {
        return videoRepository.findById(id);
    }
    
    @Transactional
    public Video save(Video video) {
        return videoRepository.save(video);
    }
    
    @Transactional
    public VideoDTO updateThumbnail(Long id, byte[] thumbnailData) throws IOException {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + id));
        
        // Delete old thumbnail
        try {
            Files.deleteIfExists(Paths.get(video.getCoverImagePath()));
        } catch (IOException e) {
            // Log error but continue
        }
        
        // Save new thumbnail
        String thumbnailFileName = fileUtils.generateFileName("thumbnail" + System.currentTimeMillis() + ".jpg");
        Path thumbnailFilePath = Paths.get(uploadDir, thumbnailFileName);
        Files.write(thumbnailFilePath, thumbnailData);
        
        video.setCoverImagePath(thumbnailFilePath.toString());
        video = videoRepository.save(video);
        
        return convertToDTO(video, null);
    }
}