package com.lvslab.mfr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transport_order")
public class TransportOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "storage_order_id", nullable = false, unique = true)
    private Long storageOrderId;

    @Column(name = "load_carrier_id", nullable = false)
    private Long loadCarrierId;

    @Column(name = "source_location_id")
    private Long sourceLocationId;

    @Column(name = "target_location_id", nullable = false)
    private Long targetLocationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransportOrderStatus status = TransportOrderStatus.CREATED;

    public TransportOrder() {}

    public TransportOrder(Long storageOrderId, Long loadCarrierId, Long sourceLocationId, Long targetLocationId) {
        this.storageOrderId   = storageOrderId;
        this.loadCarrierId    = loadCarrierId;
        this.sourceLocationId = sourceLocationId;
        this.targetLocationId = targetLocationId;
    }

    public Long getId()                              { return id; }
    public Long getStorageOrderId()                  { return storageOrderId; }
    public Long getLoadCarrierId()                   { return loadCarrierId; }
    public Long getSourceLocationId()                { return sourceLocationId; }
    public Long getTargetLocationId()                { return targetLocationId; }
    public TransportOrderStatus getStatus()          { return status; }
    public void setStatus(TransportOrderStatus s)    { this.status = s; }
}
