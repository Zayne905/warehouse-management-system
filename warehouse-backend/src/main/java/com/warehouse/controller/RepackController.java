package com.warehouse.controller;

import com.warehouse.model.dto.*;
import com.warehouse.service.RepackService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class RepackController {

    private final RepackService repackService;

    public RepackController(RepackService repackService) {
        this.repackService = repackService;
    }

    /** 转包单列表 */
    @PostMapping("/repack/list")
    public Result<PageResult<RepackOrderVO>> list(@RequestBody RepackOrderQuery query) {
        return Result.ok(repackService.listRepackOrders(query));
    }

    /** 转包单详情 */
    @PostMapping("/repack/detail")
    public Result<RepackOrderVO> detail(@RequestBody Map<String, Long> body) {
        return Result.ok(repackService.getRepackDetail(body.get("id")));
    }

    /** 创建转包单（支持空明细，后续逐行扫码添加） */
    @PostMapping("/repack/save")
    public Result<RepackOrderVO> save(@RequestBody RepackOrderSaveDTO dto) {
        if (dto.getId() != null) {
            throw new RuntimeException("转包单创建后不可编辑头信息，请新建或取消后重新创建");
        }
        return Result.ok(repackService.createRepackOrder(dto));
    }

    /** 向已有转包单添加一行明细（扫码或手动输入） */
    @PostMapping("/repack/add-detail")
    public Result<RepackOrderVO> addDetail(@RequestBody Map<String, Object> body) {
        Long orderId = ((Number) body.get("orderId")).longValue();
        String sourceKanbanNo = (String) body.get("sourceKanbanNo");
        Integer transferQty = ((Number) body.get("transferQty")).intValue();
        return Result.ok(repackService.addDetailLine(orderId, sourceKanbanNo, transferQty));
    }

    /** 删除转包单的一行明细 */
    @PostMapping("/repack/remove-detail")
    public Result<RepackOrderVO> removeDetail(@RequestBody Map<String, Object> body) {
        Long detailId = ((Number) body.get("detailId")).longValue();
        return Result.ok(repackService.removeDetailLine(detailId));
    }

    /** 向下转包：输入目标箱容量自动生成拆分行 */
    @PostMapping("/repack/breakdown-generate")
    public Result<RepackOrderVO> breakdownGenerate(@RequestBody Map<String, Object> body) {
        Long orderId = ((Number) body.get("orderId")).longValue();
        Integer capacity = ((Number) body.get("targetBoxCapacity")).intValue();
        return Result.ok(repackService.breakdownAdd(orderId, capacity));
    }

    /** 删除转包单 */
    @PostMapping("/repack/delete")
    public Result<?> delete(@RequestBody Map<String, Long> body) {
        repackService.deleteRepack(body.get("id"));
        return Result.ok(null);
    }

    /** 取消转包单 */
    @PostMapping("/repack/cancel")
    public Result<?> cancel(@RequestBody Map<String, Long> body) {
        repackService.cancelRepack(body.get("id"));
        return Result.ok(null);
    }

    /** 确认执行转包（核心接口） */
    @PostMapping("/repack/confirm")
    public Result<RepackOrderVO> confirm(@RequestBody Map<String, Long> body) {
        return Result.ok(repackService.confirmRepack(body.get("id")));
    }

    /** 扫码预览源包装信息 */
    @PostMapping("/repack/preview")
    public Result<Map<String, Object>> preview(@RequestBody Map<String, String> body) {
        return Result.ok(repackService.previewRepack(body.get("kanbanNo")));
    }

    /** 追溯看板转包链 */
    @PostMapping("/repack/trace")
    public Result<RepackRelationVO> trace(@RequestBody Map<String, String> body) {
        return Result.ok(repackService.traceByKanban(body.get("kanbanNo")));
    }
}
