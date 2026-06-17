package com.warehouse.model.enums;

public enum OutboundStatus {
    PENDING_OUT(0, "未出库"),
    PARTIAL_OUT(1, "部分出库"),
    COMPLETED(2, "已出库"),
    CANCELLED(3, "作废");

    private final int code;
    private final String label;

    OutboundStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() { return code; }
    public String getLabel() { return label; }

    public static String getLabelByCode(int code) {
        for (OutboundStatus s : values()) {
            if (s.code == code) return s.label;
        }
        return "未知";
    }

    /** 是否允许编辑：作废不可编辑，其他都可以 */
    public static boolean canEdit(int status, boolean isAdmin) {
        return status != CANCELLED.getCode();
    }
}
