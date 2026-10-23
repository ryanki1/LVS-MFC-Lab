package com.lvslab.lvs.api

import com.lvslab.lvs.domain.ResourceNotFound
import com.lvslab.lvs.domain.StorageOrder
import jakarta.ejb.Stateless
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Stateless
@Path("/storage-orders")
@Produces(MediaType.APPLICATION_JSON)
class StorageOrderResource {

    @PersistenceContext(unitName = "LvsPU")
    private lateinit var em: EntityManager

    @GET
    @Path("/{id}")
    fun getStorageOrder(@PathParam("id") id: Long): StorageOrderResponse {
        val order = em.find(StorageOrder::class.java, id)
            ?: throw ResourceNotFound("StorageOrder", id)
        return order.toResponse()
    }

    private fun StorageOrder.toResponse() = StorageOrderResponse(
        id = id!!,
        loadCarrierCode = loadCarrier?.carrierCode ?: "",
        targetLocationId = targetLocation?.id ?: 0,
        status = status.name
    )
}
