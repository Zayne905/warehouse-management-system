package com.warehouse.model.dto;

import lombok.Data;

@Data
public class RepackScanDTO {
    /** 转包单ID */
    private Long repackOrderId;
    /** 源看板号（扫码输入） */
    private String sourceKanbanNo;
    /** 转出数量 */
    private Integer transferQty;
    /** 目标看板号（可选，手工输入或扫描空器具） */
    private String targetKanbanNo;
    /** 操作人ID */
    private Long operatorId;
}
