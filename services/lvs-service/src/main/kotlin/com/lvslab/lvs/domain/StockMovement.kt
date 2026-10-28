package com.lvslab.lvs.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "stock_movement")
class StockMovement(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "load_carrier_id", nullable = false)
    var loadCarrier: LoadCarrier? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    var article: Article? = null,
    @Column(nullable = false)
    var quantity: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_location_id")
    var sourceLocation: StorageLocation? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_location_id", nullable = false)
    var targetLocation: StorageLocation? = null,
    @Column(name = "moved_at", nullable = false)
    var movedAt: Instant = Instant.now()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        protected set
}