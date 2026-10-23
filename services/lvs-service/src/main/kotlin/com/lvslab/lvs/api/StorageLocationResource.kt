package com.lvslab.lvs.api

import com.lvslab.lvs.domain.LocationStatus
import com.lvslab.lvs.domain.StorageLocation
import jakarta.ejb.Stateless
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Stateless
@Path("/locations")
@Produces(MediaType.APPLICATION_JSON)
class StorageLocationResource {

    @PersistenceContext(unitName = "LvsPU")
    private lateinit var em: EntityManager

    @GET
    fun getLocations(@QueryParam("status") statusParam: String?): List<StorageLocationResponse> {
        val query = if (statusParam != null) {
            val status = LocationStatus.valueOf(statusParam.uppercase())
            em.createQuery(
                "SELECT l FROM StorageLocation l WHERE l.status = :status ORDER BY l.aisle, l.level, l.slot",
                StorageLocation::class.java
            ).setParameter("status", status)
        } else {
            em.createQuery(
                "SELECT l FROM StorageLocation l ORDER BY l.aisle, l.level, l.slot",
                StorageLocation::class.java
            )
        }
        return query.resultList.map { it.toResponse() }
    }

    private fun StorageLocation.toResponse() = StorageLocationResponse(
        id = id!!,
        aisle = aisle,
        level = level,
        slot = slot,
        type = type,
        status = status.name
    )
}
