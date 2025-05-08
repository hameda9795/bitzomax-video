package com.bitzomax.controller;

import com.bitzomax.entity.Tag;
import com.bitzomax.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping("/popular")
    public ResponseEntity<List<String>> getPopularTags(
            @RequestParam(defaultValue = "20") int limit) {
        
        List<Tag> popularTags = tagService.getMostPopularTags(limit);
        List<String> tagNames = popularTags.stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(tagNames);
    }
}