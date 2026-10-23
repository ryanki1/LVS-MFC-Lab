package com.lvslab.lvs.api

data class AsnRequest(
    val noticeNumber: String = "",
    val positions: List<AsnPositionRequest> = emptyList()
)

data class AsnPositionRequest(
    val articleNumber: String = "",
    val expectedQuantity: Int = 0
)

data class AsnResponse(
    val id: Long,
    val noticeNumber: String
)

data class GoodsReceiptRequest(
    val noticeNumber: String = "",
    val articleNumber: String = "",
    val carrierCode: String = "",
    val quantity: Int = 0
)

data class GoodsReceiptResponse(
    val storageOrderId: Long,
    val targetLocationId: Long
)

data class StorageLocationResponse(
    val id: Long,
    val aisle: Int,
    val level: Int,
    val slot: Int,
    val type: String,
    val status: String
)

data class StorageOrderResponse(
    val id: Long,
    val loadCarrierCode: String,
    val targetLocationId: Long,
    val status: String
)

data class ErrorResponse(
    val message: String
)
