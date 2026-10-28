package com.lvslab.lvs.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "storage_location")
class StorageLocation(
    @Column(nullable = false)
    var aisle: Int = 0,
    @Column(nullable = false)
    var level: Int = 0,
    @Column(nullable = false)
    var slot: Int = 0,
    @Column(name = "location_type", nullable = false, length = 40)
    var type: String = "STANDARD",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: LocationStatus = LocationStatus.FREE
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        protected set
}