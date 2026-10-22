package com.lvslab.lvs.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SlottingStrategyTest {
    private val strategy = SlottingStrategy()

    @Test
    fun `reserves first matching free location in aisle with most matching free locations`() {
        val locations = listOf(
            StorageLocation(aisle = 1, level = 1, slot = 1),
            StorageLocation(aisle = 2, level = 2, slot = 1),
            StorageLocation(aisle = 2, level = 1, slot = 2),
            StorageLocation(aisle = 2, level = 1, slot = 1),
            StorageLocation(aisle = 2, level = 1, slot = 3, type = "COLD"),
            StorageLocation(aisle = 2, level = 1, slot = 4, status = LocationStatus.OCCUPIED)
        )

        val reserved = strategy.reserveLocation(locations, "STANDARD")

        assertEquals(2, reserved.aisle)
        assertEquals(1, reserved.level)
        assertEquals(1, reserved.slot)
        assertEquals(LocationStatus.RESERVED, reserved.status)
    }

    @Test
    fun `uses lowest numbered aisle when free location counts tie`() {
        val locations = listOf(
            StorageLocation(aisle = 2, level = 1, slot = 1),
            StorageLocation(aisle = 1, level = 1, slot = 1)
        )

        val reserved = strategy.reserveLocation(locations, "STANDARD")

        assertEquals(1, reserved.aisle)
        assertEquals(LocationStatus.RESERVED, reserved.status)
    }

    @Test
    fun `throws a business error when no matching free location exists`() {
        val locations = listOf(
            StorageLocation(aisle = 1, status = LocationStatus.OCCUPIED)
        )

        assertThrows(NoAvailableStorageLocation::class.java) {
            strategy.reserveLocation(locations, "STANDARD")
        }
    }
}