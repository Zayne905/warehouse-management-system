package com.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehouse.model.entity.Part;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PartMapper extends BaseMapper<Part> {
}
