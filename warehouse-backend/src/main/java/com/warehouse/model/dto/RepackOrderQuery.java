package com.warehouse.model.dto;

import lombok.Data;

@Data
public class RepackOrderQuery {
    private Integer current;
    private Integer size;
    private String orderNo;
    private Integer status;
    private String repackType;
}
