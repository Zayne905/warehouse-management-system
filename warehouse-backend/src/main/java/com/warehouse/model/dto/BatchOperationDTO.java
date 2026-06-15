package com.warehouse.model.dto;

import lombok.Data;

@Data
public class BatchOperationDTO {
    private Long targetOrderId;
    private Long sourceOrderId;
    private Long warehouseAreaId;
    private java.util.List<Long> detailIds;
}
