package com.cortex.lab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cortex.lab.entity.Discussion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiscussionMapper extends BaseMapper<Discussion> {
}
