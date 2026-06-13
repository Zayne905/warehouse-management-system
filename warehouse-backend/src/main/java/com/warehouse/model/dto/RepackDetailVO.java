package com.warehouse.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RepackDetailVO {
    private Long id;
    private Long repackOrderId;
    private Long sourceKanbanId;
    private String sourceKanbanNo;
    private Integer sourceOriginalQty;
    private String targetKanbanNo;
    private Long partId;
    private String partCode;
    private String partName;
    private Integer transferQty;
    private Integer remainderQty;
    private Integer sourceRemainingQty;
    private Integer lineNo;
    private LocalDateTime createTime;
}
