package com.lvslab.lvs.domain

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class SlottingStrategy {
    fun reserveLocation(locations: Collection<StorageLocation>, requiredType: String): StorageLocation {
        val matchingFreeLocations = locations
            .asSequence()
            .filter { it.status == LocationStatus.FREE && it.type == requiredType }
            .toList()

        val selectedAisle = matchingFreeLocations
            .groupingBy { it.aisle }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()
            ?.key
            ?: throw NoAvailableStorageLocation(requiredType)

        return matchingFreeLocations
            .asSequence()
            .filter { it.aisle == selectedAisle }
            .sortedWith(compareBy<StorageLocation> { it.level }.thenBy { it.slot })
            .first()
            .also { it.status = LocationStatus.RESERVED }
    }
}