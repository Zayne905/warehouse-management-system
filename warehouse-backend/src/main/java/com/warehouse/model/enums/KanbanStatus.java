package com.warehouse.model.enums;

public enum KanbanStatus {
    PENDING_INBOUND(0, "待入库"),
    AVAILABLE(1, "在库可用"),
    LOCKED(2, "待出库"),
    OUTBOUND(3, "已出库"),
    BLOCKED(4, "封存"),
    PARTIAL_REPACK(5, "部分转出"),
    CLEARED(6, "已清空");

    private final int code;
    private final String label;

    KanbanStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static String getLabelByCode(int code) {
        for (KanbanStatus s : values()) {
            if (s.code == code) {
                return s.label;
            }
        }
        return "未知";
    }
}
