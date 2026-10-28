package com.lvslab.lvs.messaging

// Ausgehende Nachrichten an den mfr-service
data class TransportRequested(
    val storageOrderId: Long,
    val loadCarrierId: Long,
    val sourceLocationId: Long?,
    val targetLocationId: Long
)

// Eingehende Ergebnisse vom mfr-service
data class TransportConfirmed(
    val storageOrderId: Long,
    val loadCarrierId: Long,
    val targetLocationId: Long
)

data class TransportFaulted(
    val storageOrderId: Long,
    val loadCarrierId: Long,
    val faultedLocationId: Long
)
