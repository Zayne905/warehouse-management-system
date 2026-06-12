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
}
