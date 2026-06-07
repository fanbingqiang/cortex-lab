package com.cortex.service;

import com.alibaba.fastjson2.JSON;
import com.cortex.dto.UserProfileDto;
import com.cortex.entity.MistakeRecord;
import com.cortex.lab.entity.User;
import com.cortex.lab.mapper.UserMapper;
import com.cortex.mapper.MistakeRecordMapper;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {

    private final UserMapper userMapper;
    private final MistakeRecordMapper mistakeRecordMapper;

    public User getOrCreateProfile(String userId, String username) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUserId, userId));
        if (user == null) {
            user = new User();
            user.setUserId(userId);
            user.setUsername(username);
            user.setPersonalityTags("[]");
            user.setWorkHabits("[]");
            user.setPreferences("{}");
            user.setGmtCreate(LocalDateTime.now());
            user.setGmtModified(LocalDateTime.now());
            userMapper.insert(user);
        }
        return user;
    }

    public UserProfileDto getProfile(String userId) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUserId, userId));
        if (user == null) {
            return null;
        }

        UserProfileDto dto = new UserProfileDto();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());

        if (StrUtil.isNotBlank(user.getPersonalityTags())) {
            dto.setPersonalityTags(JSON.parseArray(user.getPersonalityTags(), String.class));
        } else {
            dto.setPersonalityTags(new ArrayList<>());
        }

        if (StrUtil.isNotBlank(user.getWorkHabits())) {
            dto.setWorkHabits(JSON.parseArray(user.getWorkHabits(), String.class));
        } else {
            dto.setWorkHabits(new ArrayList<>());
        }

        List<MistakeRecord> mistakes = mistakeRecordMapper.selectList(
            new LambdaQueryWrapper<MistakeRecord>()
                .eq(MistakeRecord::getUserId, userId)
                .orderByDesc(MistakeRecord::getGmtCreate)
                .last("LIMIT 10")
        );

        List<UserProfileDto.MistakeDto> mistakeDtos = new ArrayList<>();
        for (MistakeRecord m : mistakes) {
            UserProfileDto.MistakeDto md = new UserProfileDto.MistakeDto();
            md.setKeyword(m.getKeyword());
            md.setDescription(m.getDescription());
            mistakeDtos.add(md);
        }
        dto.setMistakes(mistakeDtos);

        return dto;
    }

    public void addPersonalityTag(String userId, String tag) {
        User user = getOrCreateProfile(userId, "用户");
        List<String> tags = JSON.parseArray(user.getPersonalityTags(), String.class);
        if (tags == null) {
            tags = new ArrayList<>();
        }
        if (!tags.contains(tag)) {
            tags.add(tag);
            user.setPersonalityTags(JSON.toJSONString(tags));
            user.setGmtModified(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }

    public void addWorkHabit(String userId, String habit) {
        User user = getOrCreateProfile(userId, "用户");
        List<String> habits = JSON.parseArray(user.getWorkHabits(), String.class);
        if (habits == null) {
            habits = new ArrayList<>();
        }
        if (!habits.contains(habit)) {
            habits.add(habit);
            user.setWorkHabits(JSON.toJSONString(habits));
            user.setGmtModified(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }

    public void addMistake(String userId, String keyword, String description, String taskId) {
        MistakeRecord record = new MistakeRecord();
        record.setUserId(userId);
        record.setKeyword(keyword);
        record.setDescription(description);
        record.setTaskId(taskId);
        record.setGmtCreate(LocalDateTime.now());
        mistakeRecordMapper.insert(record);
    }

    public void updateProfile(String userId, List<String> personalityTags, List<String> workHabits) {
        User user = getOrCreateProfile(userId, "用户");
        if (personalityTags != null) {
            user.setPersonalityTags(JSON.toJSONString(personalityTags));
        }
        if (workHabits != null) {
            user.setWorkHabits(JSON.toJSONString(workHabits));
        }
        user.setGmtModified(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
