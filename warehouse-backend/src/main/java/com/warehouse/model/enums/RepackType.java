package com.warehouse.model.enums;

public enum RepackType {
    BREAKDOWN("BREAKDOWN", "向下转包", "1 → N（拆包）"),
    CONSOLIDATE("CONSOLIDATE", "向上转包", "N → 1（合并）"),
    REMAINDER("REMAINDER", "带余量转包", "1 → 1（平级，有余量）");

    private final String code;
    private final String label;
    private final String desc;

    RepackType(String code, String label, String desc) {
        this.code = code;
        this.label = label;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public String getDesc() { return desc; }

    public static String getLabelByCode(String code) {
        for (RepackType t : values()) {
            if (t.code.equals(code)) return t.label;
        }
        return "未知";
    }
}
