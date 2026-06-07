package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("supplier_part")
public class SupplierPart {
    private Long id;
    private Long supplierId;
    private Long partId;
}
