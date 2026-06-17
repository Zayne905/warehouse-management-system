package com.warehouse.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class InboundOrderSaveDTO {
    private Long id;
    private Long supplierId;
    private Long customerId;
    private String orderNumber;
    private String remark;
    private List<InboundDetailDTO> details;
}
