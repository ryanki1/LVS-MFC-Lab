package com.lvslab.lvs.api

import com.lvslab.lvs.domain.InvalidGoodsReceipt
import com.lvslab.lvs.domain.LvsException
import com.lvslab.lvs.domain.NoAvailableStorageLocation
import com.lvslab.lvs.domain.ResourceNotFound
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class LvsExceptionMapper : ExceptionMapper<LvsException> {

    override fun toResponse(exception: LvsException): Response {
        val status = when (exception) {
            is ResourceNotFound -> Response.Status.NOT_FOUND
            is InvalidGoodsReceipt, is NoAvailableStorageLocation -> Response.Status.CONFLICT
        }
        return Response.status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(ErrorResponse(message = exception.message ?: "Unbekannter Fehler"))
            .build()
    }
}
