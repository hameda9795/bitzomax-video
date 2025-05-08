package com.bitzomax.util;

import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.global.avcodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for converting videos to WebM format using JavaCV/FFmpeg
 */
@Component
public class WebmConverter {
    private static final Logger logger = LoggerFactory.getLogger(WebmConverter.class);

    /**
     * Converts a video to WebM format with VP9 codec for better compression
     *
     * @param videoData The original video data as byte array
     * @return The converted WebM video data as byte array
     * @throws IOException If conversion fails
     */
    public byte[] convertToWebm(byte[] videoData) throws IOException {
        File tempInputFile = File.createTempFile("input_", ".mp4");
        File tempOutputFile = File.createTempFile("output_", ".webm");

        try {
            // Write input data to temp file
            try (FileOutputStream fos = new FileOutputStream(tempInputFile)) {
                fos.write(videoData);
            }

            // Configure input
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tempInputFile);
            grabber.start();

            // Configure output
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(tempOutputFile,
                    grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
            
            // Set WebM/VP9 encoding parameters
            recorder.setFormat("webm");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_VP9);
            recorder.setVideoQuality(30); // Lower is better quality (0-51)
            recorder.setFrameRate(grabber.getFrameRate());
            recorder.setVideoBitrate((int)(grabber.getVideoBitrate() * 0.8)); // 80% of original bitrate
            
            // Audio settings
            if (grabber.getAudioChannels() > 0) {
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_OPUS);
                recorder.setAudioBitrate(128000); // 128 kbps
                recorder.setSampleRate(grabber.getSampleRate());
                recorder.setAudioChannels(grabber.getAudioChannels());
            }
            
            // Add frame options for VP9
            recorder.setVideoOption("crf", "30");       // Constant Rate Factor (quality)
            recorder.setVideoOption("deadline", "good"); // Encoding speed/quality tradeoff
            recorder.setVideoOption("cpu-used", "2");   // CPU usage (0-5), higher = faster but lower quality
            recorder.setVideoOption("row-mt", "1");     // Row-based multithreading
            
            recorder.start();

            // Process frames
            Frame frame;
            while ((frame = grabber.grabFrame()) != null) {
                recorder.record(frame);
            }

            // Close resources
            recorder.stop();
            grabber.stop();

            // Read the output file
            return Files.readAllBytes(Path.of(tempOutputFile.getAbsolutePath()));
            
        } catch (Exception e) {
            logger.error("Error converting video to WebM: {}", e.getMessage(), e);
            throw new IOException("Failed to convert video to WebM: " + e.getMessage(), e);
        } finally {
            // Clean up temporary files
            try {
                Files.deleteIfExists(tempInputFile.toPath());
                Files.deleteIfExists(tempOutputFile.toPath());
            } catch (IOException e) {
                logger.warn("Failed to delete temporary files: {}", e.getMessage());
            }
        }
    }
}