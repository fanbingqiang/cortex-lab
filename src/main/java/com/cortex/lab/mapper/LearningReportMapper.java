package com.cortex.lab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cortex.lab.entity.LearningReport;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningReportMapper extends BaseMapper<LearningReport> {
    // 学习报表 Mapper
}
