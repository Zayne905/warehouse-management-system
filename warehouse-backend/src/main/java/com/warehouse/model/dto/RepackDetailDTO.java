package com.warehouse.model.dto;

import lombok.Data;

@Data
public class RepackDetailDTO {
    private Long id;
    private Long sourceKanbanId;
    private String sourceKanbanNo;
    private Long partId;
    private String partCode;
    private String partName;
    private Integer transferQty;
    private Integer remainderQty;
    private Integer lineNo;
    // 以下字段由系统在确认转包时填充
    private String targetKanbanNo;
    private Integer sourceRemainingQty;
}
