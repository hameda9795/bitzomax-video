package com.bitzomax.service;

import com.bitzomax.entity.Tag;
import com.bitzomax.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    @Transactional(readOnly = true)
    public List<Tag> getMostPopularTags(int limit) {
        return tagRepository.findMostUsedTags(limit);
    }

    @Transactional
    public Set<Tag> getOrCreateTags(Set<String> tagNames) {
        Set<Tag> tags = new HashSet<>();

        if (tagNames != null && !tagNames.isEmpty()) {
            for (String tagName : tagNames) {
                // Normalize tag name to lowercase
                String normalizedTagName = tagName.toLowerCase().trim();
                if (!normalizedTagName.isEmpty()) {
                    Tag tag = tagRepository.findByName(normalizedTagName)
                            .orElseGet(() -> {
                                Tag newTag = new Tag();
                                newTag.setName(normalizedTagName);
                                return tagRepository.save(newTag);
                            });
                    tags.add(tag);
                }
            }
        }

        return tags;
    }

    public List<Tag> findByTagNames(List<String> tagNames) {
        return tagRepository.findByTagNames(tagNames);
    }
}