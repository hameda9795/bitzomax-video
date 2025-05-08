package com.bitzomax.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadDTO {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String poemText;
    private boolean premium;
    private Set<String> tags;
    
    // SEO fields
    private String seoTitle;
    private String seoDescription;
    private String seoKeywords;
}