package com.bitzomax.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoRequest {
    @NotBlank(message = "Title cannot be empty")
    private String title;
    
    private String description;
    
    private String poemText;
    
    private String url;
    
    private String thumbnailUrl;
    
    @NotNull(message = "Premium status must be specified")
    private Boolean premium;
    
    private Set<String> tags;
    
    private String seoTitle;
    
    private String seoDescription;
    
    private String seoKeywords;
}