package com.lvslab.mfr.domain;

// Java record: kompakter als data class, aber kein no-arg-Konstruktor – für JPA ungeeignet.
// Für DTOs (reine Datentransfer-Objekte) ist record ideal.
public record TransportRequested(
        Long storageOrderId,
        Long loadCarrierId,
        Long sourceLocationId,
        Long targetLocationId
) {}
