package com.lvslab.lvs.api

import com.lvslab.lvs.domain.AdvanceShippingNotice
import com.lvslab.lvs.domain.InvalidGoodsReceipt
import com.lvslab.lvs.domain.LoadCarrier
import com.lvslab.lvs.domain.ResourceNotFound
import com.lvslab.lvs.domain.SlottingStrategy
import com.lvslab.lvs.domain.StorageOrder
import com.lvslab.lvs.messaging.JmsSender
import com.lvslab.lvs.messaging.TransportRequested
import jakarta.ejb.Stateless
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Stateless
@Path("/goods-receipt")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class GoodsReceiptResource {

    @PersistenceContext(unitName = "LvsPU")
    private lateinit var em: EntityManager

    @Inject
    private lateinit var slottingStrategy: SlottingStrategy

    @Inject
    private lateinit var jmsSender: JmsSender

    @POST
    fun receiveGoods(request: GoodsReceiptRequest): Response {
        val asn = em.createQuery(
            "SELECT a FROM AdvanceShippingNotice a WHERE a.noticeNumber = :nr",
            AdvanceShippingNotice::class.java
        ).setParameter("nr", request.noticeNumber).resultList.firstOrNull()
            ?: throw ResourceNotFound("AdvanceShippingNotice", request.noticeNumber)

        val position = asn.positions.find { it.article?.articleNumber == request.articleNumber }
            ?: throw InvalidGoodsReceipt(
                "Artikel '${request.articleNumber}' ist nicht in Avis '${request.noticeNumber}' enthalten."
            )

        if (position.receivedQuantity + request.quantity > position.expectedQuantity) {
            throw InvalidGoodsReceipt(
                "Überlieferung: erwartet ${position.expectedQuantity}, " +
                    "bereits erfasst ${position.receivedQuantity}, eingehend ${request.quantity}."
            )
        }

        val article = position.article!!
        val loadCarrier = LoadCarrier(
            carrierCode = request.carrierCode,
            article = article,
            quantity = request.quantity
        )
        em.persist(loadCarrier)

        val allLocations = em.createQuery(
            "SELECT l FROM StorageLocation l WHERE l.type = :type AND l.status = 'FREE'",
            com.lvslab.lvs.domain.StorageLocation::class.java
        )
            .setParameter("type", article.storageType)
            .resultList

        val targetLocation = slottingStrategy.reserveLocation(allLocations, article.storageType)

        position.receivedQuantity += request.quantity

        val storageOrder = StorageOrder(
            noticePosition = position,
            loadCarrier = loadCarrier,
            targetLocation = targetLocation
        )
        em.persist(storageOrder)

        // JMS-Nachricht in derselben JTA-Transaktion senden –
        // bei DB-Fehler wird auch die JMS-Nachricht zurückgerollt.
        jmsSender.sendTransportRequested(
            TransportRequested(
                storageOrderId   = storageOrder.id,
                loadCarrierId    = loadCarrier.id,
                sourceLocationId = null,
                targetLocationId = targetLocation.id
            )
        )

        return Response.status(Response.Status.CREATED)
            .entity(GoodsReceiptResponse(storageOrderId = storageOrder.id, targetLocationId = targetLocation.id))
            .build()
    }
}
