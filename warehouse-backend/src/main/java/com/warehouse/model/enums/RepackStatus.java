package com.warehouse.model.enums;

public enum RepackStatus {
    PENDING(0, "待转包"),
    COMPLETED(1, "已完成"),
    CANCELLED(2, "已取消");

    private final int code;
    private final String label;

    RepackStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() { return code; }
    public String getLabel() { return label; }

    public static String getLabelByCode(int code) {
        for (RepackStatus s : values()) {
            if (s.code == code) return s.label;
        }
        return "未知";
    }
}
