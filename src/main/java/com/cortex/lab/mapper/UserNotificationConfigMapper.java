package com.cortex.lab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cortex.lab.entity.UserNotificationConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserNotificationConfigMapper extends BaseMapper<UserNotificationConfig> {
    // 用户通知配置 Mapper
}
