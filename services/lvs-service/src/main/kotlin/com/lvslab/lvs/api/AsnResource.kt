package com.lvslab.lvs.api

import com.lvslab.lvs.domain.AdvanceShippingNotice
import com.lvslab.lvs.domain.AdvanceShippingNoticePosition
import com.lvslab.lvs.domain.ResourceNotFound
import jakarta.ejb.Stateless
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Stateless
@Path("/asn")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class AsnResource {

    @PersistenceContext(unitName = "LvsPU")
    private lateinit var em: EntityManager

    @POST
    fun createAsn(request: AsnRequest): Response {
        val asn = AdvanceShippingNotice(noticeNumber = request.noticeNumber)

        request.positions.forEach { posReq ->
            val article = em.createQuery(
                "SELECT a FROM Article a WHERE a.articleNumber = :nr", com.lvslab.lvs.domain.Article::class.java
            ).setParameter("nr", posReq.articleNumber).resultList.firstOrNull()
                ?: throw ResourceNotFound("Article", posReq.articleNumber)

            val position = AdvanceShippingNoticePosition(
                notice = asn,
                article = article,
                expectedQuantity = posReq.expectedQuantity
            )
            asn.positions.add(position)
        }

        em.persist(asn)
        return Response.status(Response.Status.CREATED)
            .entity(AsnResponse(id = asn.id!!, noticeNumber = asn.noticeNumber))
            .build()
    }
}
