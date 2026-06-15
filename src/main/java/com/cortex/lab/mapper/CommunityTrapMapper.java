package com.cortex.lab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cortex.lab.entity.CommunityTrap;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommunityTrapMapper extends BaseMapper<CommunityTrap> {
    // 社区陷阱代码 Mapper
}