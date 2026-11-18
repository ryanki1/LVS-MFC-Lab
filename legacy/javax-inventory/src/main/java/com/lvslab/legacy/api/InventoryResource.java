package com.lvslab.legacy.api;

import com.lvslab.legacy.domain.InventoryItem;
import com.lvslab.legacy.service.InventoryService;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Stateless
@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    @Inject
    private InventoryService inventoryService;

    @GET
    public List<InventoryItem> getAll() {
        return inventoryService.findAll();
    }

    @GET
    @Path("/{articleNumber}")
    public Response getByArticleNumber(@PathParam("articleNumber") String articleNumber) {
        InventoryItem item = inventoryService.findByArticleNumber(articleNumber);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(item).build();
    }

    @POST
    public Response create(InventoryItem item) {
        return Response.status(Response.Status.CREATED)
                       .entity(inventoryService.save(item))
                       .build();
    }
}
