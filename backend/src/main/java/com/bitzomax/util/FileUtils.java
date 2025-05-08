package com.bitzomax.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileUtils {

    /**
     * Generates a unique filename for uploaded files
     * 
     * @param originalFilename The original filename
     * @return A unique filename based on UUID
     */
    public String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
    
    /**
     * Converts a video file to WebM format to reduce file size
     * 
     * @param inputFile The input video file
     * @param outputDir The output directory
     * @return The path to the converted file
     * @throws IOException If there's an error processing the file
     */
    public String convertToWebM(MultipartFile inputFile, String outputDir) throws IOException {
        // Create a temporary file to hold the uploaded content
        Path tempFile = Files.createTempFile("upload-", inputFile.getOriginalFilename());
        inputFile.transferTo(tempFile.toFile());

        // Generate output filename
        String outputFileName = generateFileName("output.webm");
        Path outputPath = Paths.get(outputDir, outputFileName);
        
        // Construct FFmpeg command
        String ffmpegCommand = String.format(
            "ffmpeg -i \"%s\" -c:v libvpx-vp9 -crf 30 -b:v 0 -c:a libopus -b:a 128k \"%s\"",
            tempFile.toString(), outputPath.toString()
        );
        
        // Execute the command
        Process process = Runtime.getRuntime().exec(ffmpegCommand);
        try {
            // Wait for conversion to complete
            int exitCode = process.waitFor();
            
            // Clean up the temporary file
            Files.deleteIfExists(tempFile);
            
            if (exitCode != 0) {
                throw new IOException("FFmpeg conversion failed with exit code: " + exitCode);
            }
            
            return outputPath.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Video conversion was interrupted", e);
        }
    }
}