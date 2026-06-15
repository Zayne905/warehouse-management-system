package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.InboundOrderMapper;
import com.warehouse.model.entity.InboundOrder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class OrderNoGenerator {

    private final InboundOrderMapper inboundOrderMapper;

    public OrderNoGenerator(InboundOrderMapper inboundOrderMapper) {
        this.inboundOrderMapper = inboundOrderMapper;
    }

    public synchronized String generate() {
        String todayPrefix = "R" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 使用原始 SQL 条件查询今天的所有入库单
        QueryWrapper<InboundOrder> wrapper = new QueryWrapper<>();
        wrapper.apply("order_no LIKE CONCAT({0}, '%')", todayPrefix)
               .orderByDesc("order_no");

        List<InboundOrder> list = inboundOrderMapper.selectList(wrapper);
        System.out.println("ORDER_NO_GEN: prefix=" + todayPrefix + ", found=" + (list != null ? list.size() : 0));

        int seq = 1;
        if (list != null && !list.isEmpty()) {
            // 取第一个（已按 order_no DESC 排序）
            String lastNo = list.get(0).getOrderNo();
            System.out.println("ORDER_NO_GEN: last=" + lastNo);
            if (lastNo != null && lastNo.length() >= 12) {
                try {
                    seq = Integer.parseInt(lastNo.substring(9)) + 1;
                } catch (NumberFormatException e) {
                    seq = 1;
                }
            }
        }

        String result = todayPrefix + String.format("%03d", seq);
        System.out.println("ORDER_NO_GEN: result=" + result);
        return result;
    }
}
