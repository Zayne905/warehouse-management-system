package com.warehouse.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long current;
    private long size;

    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        PageResult<T> r = new PageResult<>();
        r.records = records;
        r.total = total;
        r.current = current;
        r.size = size;
        return r;
    }
}
