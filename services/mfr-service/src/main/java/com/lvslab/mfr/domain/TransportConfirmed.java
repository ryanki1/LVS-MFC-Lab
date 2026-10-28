package com.lvslab.mfr.domain;

public record TransportConfirmed(
        Long storageOrderId,
        Long loadCarrierId,
        Long targetLocationId
) {}
