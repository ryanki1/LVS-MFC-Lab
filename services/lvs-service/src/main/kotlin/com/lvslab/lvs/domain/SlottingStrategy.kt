package com.lvslab.lvs.domain

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class SlottingStrategy {
    fun reserveLocation(locations: Collection<StorageLocation>, requiredType: String): StorageLocation {

        val selectedAisle = locations
            .groupingBy { it.aisle }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()
            ?.key
            ?: throw NoAvailableStorageLocation(requiredType)

        return locations
            .asSequence()
            .filter { it.aisle == selectedAisle }
            .sortedWith(compareBy<StorageLocation> { it.level }.thenBy { it.slot })
            .first()
            .also { it.status = LocationStatus.RESERVED }
    }
}