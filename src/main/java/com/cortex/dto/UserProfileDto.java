package com.cortex.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserProfileDto {
    private String userId;
    private String username;
    private List<String> personalityTags;
    private List<String> workHabits;
    private List<MistakeDto> mistakes;
    
    @Data
    public static class MistakeDto {
        private String keyword;
        private String description;
    }
}
