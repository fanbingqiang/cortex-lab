package com.cortex.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cortex.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
