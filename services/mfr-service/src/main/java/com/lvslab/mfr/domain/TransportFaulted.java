package com.lvslab.mfr.domain;

public record TransportFaulted(
        Long storageOrderId,
        Long loadCarrierId,
        Long faultedLocationId
) {}
