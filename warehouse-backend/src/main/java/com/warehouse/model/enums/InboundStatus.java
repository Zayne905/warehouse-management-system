package com.warehouse.model.enums;

public enum InboundStatus {
    PENDING(0, "未入库"),
    PARTIAL(1, "部分入库"),
    COMPLETED(2, "已入库"),
    CANCELLED(3, "作废");

    private final int code;
    private final String label;

    InboundStatus(int code, String label) {
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
        for (InboundStatus s : values()) {
            if (s.code == code) {
                return s.label;
            }
        }
        return "未知";
    }

    public static boolean canEdit(int status, boolean isAdmin) {
        if (status == CANCELLED.code || status == COMPLETED.code) {
            return false;
        }
        return true;
    }
}
