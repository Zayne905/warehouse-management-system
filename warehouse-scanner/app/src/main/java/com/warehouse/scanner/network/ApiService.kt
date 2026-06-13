package com.warehouse.scanner.network

import com.warehouse.scanner.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ==================== 认证 ====================

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResult<LoginData>

    // ==================== 扫码 ====================

    @POST("scan/check-duplicate")
    suspend fun checkDuplicate(@Body request: ScanCheckRequest): ApiResult<ScanCheckResult>

    @POST("scan/submit")
    suspend fun submitScan(@Body request: ScanSubmitRequest): ApiResult<ScanSubmitResult>

    @POST("scan/kanban")
    suspend fun scanKanban(@Body request: KanbanScanRequest): ApiResult<KanbanScanResult>

    @POST("outbound/scan")
    suspend fun scanOutbound(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResult<OutboundScanResult>

    @POST("kanban/toggle-block")
    suspend fun toggleBlock(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResult<ToggleBlockResult>

    @POST("scan/feedback")
    suspend fun getScanFeedback(@Body request: ScanFeedbackRequest): ApiResult<Map<String, Any>>

    @POST("scan/list")
    suspend fun listScans(@Body body: Map<String, Long>): ApiResult<List<ScanRecordVO>>

    @POST("scan/delete")
    suspend fun deleteScan(@Body body: Map<String, Long>): ApiResult<Nothing>

    // ==================== 入库单 ====================

    @POST("inbound-order/detail")
    suspend fun getInboundOrderDetail(@Body body: Map<String, Long>): ApiResult<InboundOrderVO>

    @POST("inbound-order/detail-by-no")
    suspend fun getInboundOrderDetailByNo(@Body body: Map<String, String>): ApiResult<InboundOrderVO>

    // ==================== 转包 ====================

    @POST("repack/preview")
    suspend fun repackPreview(@Body body: Map<String, String>): ApiResult<RepackPreviewData>

    @POST("repack/add-detail")
    suspend fun repackAddDetail(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResult<RepackOrderData>

    @POST("repack/save")
    suspend fun repackCreate(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResult<RepackOrderData>

    @POST("repack/confirm")
    suspend fun repackConfirm(@Body body: Map<String, Long>): ApiResult<RepackOrderData>

    // ==================== 溯源 ====================

    @POST("repack/trace")
    suspend fun repackTrace(@Body body: Map<String, String>): ApiResult<TraceData>
}
