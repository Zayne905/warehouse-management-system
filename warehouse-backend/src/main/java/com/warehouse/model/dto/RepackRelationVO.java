package com.warehouse.model.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RepackRelationVO {
    /** 当前看板号 */
    private String kanbanNo;
    /** 当前看板信息 */
    private KanbanInfo currentKanban;
    /** 向上追溯链（父→祖父→...） */
    private List<RepackRelationNode> parentChain;
    /** 向下追溯链（子→孙→...） */
    private List<RepackRelationNode> childChain;

    @Data
    public static class KanbanInfo {
        private String kanbanNo;
        private String partCode;
        private String partName;
        private Integer quantity;
        private String warehouseAreaName;
        private String supplierName;
        private String statusText;
        private String inboundOrderNo;
        private LocalDateTime createTime;
    }

    @Data
    public static class RepackRelationNode {
        private Long relationId;
        private String parentKanbanNo;
        private String childKanbanNo;
        private String repackOrderNo;
        private Integer transferQty;
        private String partCode;
        private String partName;
        private LocalDateTime repackTime;
        private Integer level; // 追溯层级(1=直接父子,2=祖孙...)
    }
}
