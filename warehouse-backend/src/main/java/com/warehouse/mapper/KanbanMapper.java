package com.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehouse.model.entity.Kanban;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KanbanMapper extends BaseMapper<Kanban> {
}
