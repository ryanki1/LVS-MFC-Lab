package com.lvslab.lvs.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "storage_order")
class StorageOrder(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asn_position_id", nullable = false)
    var noticePosition: AdvanceShippingNoticePosition? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "load_carrier_id", nullable = false)
    var loadCarrier: LoadCarrier? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_location_id", nullable = false)
    var targetLocation: StorageLocation? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: StorageOrderStatus = StorageOrderStatus.CREATED
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        protected set
}