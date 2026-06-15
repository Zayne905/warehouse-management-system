package com.warehouse.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class InboundOrderSaveDTO {
    private Long id;
    private Long supplierId;
    private String orderNumber;
    private String remark;
    private List<InboundDetailDTO> details;
}
