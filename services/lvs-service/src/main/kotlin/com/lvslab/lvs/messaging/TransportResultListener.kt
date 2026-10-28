package com.lvslab.lvs.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.lvslab.lvs.domain.LocationStatus
import com.lvslab.lvs.domain.SlottingStrategy
import com.lvslab.lvs.domain.StockMovement
import com.lvslab.lvs.domain.StorageLocation
import com.lvslab.lvs.domain.StorageOrder
import com.lvslab.lvs.domain.StorageOrderStatus
import jakarta.ejb.ActivationConfigProperty
import jakarta.ejb.MessageDriven
import jakarta.inject.Inject
import jakarta.jms.Message
import jakarta.jms.MessageListener
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Empfängt TransportConfirmed oder TransportFaulted vom mfr-service.
 *
 * Idempotenz: Vor jeder Buchung wird geprüft, ob der StorageOrder bereits
 * COMPLETED ist. Kommt dieselbe Quittierung zweimal (z.B. durch Redelivery
 * nach einem Netzwerkfehler), wird sie still verworfen.
 *
 * JTA-Transaktion: DB-Buchung und JMS-Send (bei Störung) laufen atomar.
 * Scheitert die DB, wird die Nachricht erneut zugestellt.
 */
@MessageDriven(activationConfig = [
    ActivationConfigProperty(propertyName = "destinationLookup",  propertyValue = "java:/jms/queue/TransportResults"),
    ActivationConfigProperty(propertyName = "destinationType",    propertyValue = "jakarta.jms.Queue"),
    ActivationConfigProperty(propertyName = "acknowledgeMode",    propertyValue = "Auto-acknowledge")
])
class TransportResultListener : MessageListener {

    private val log = Logger.getLogger(TransportResultListener::class.java.name)
    private val mapper = ObjectMapper()

    @PersistenceContext(unitName = "LvsPU")
    private lateinit var em: EntityManager

    @Inject
    private lateinit var slottingStrategy: SlottingStrategy

    @Inject
    private lateinit var jmsSender: JmsSender

    override fun onMessage(message: Message) {
        try {
            val body = message.getBody(String::class.java)
            // Nachrichtentyp über JSON-Felder unterscheiden
            val node = mapper.readTree(body)
            if (node.has("faultedLocationId")) {
                handleFaulted(mapper.treeToValue(node, TransportFaulted::class.java))
            } else {
                handleConfirmed(mapper.treeToValue(node, TransportConfirmed::class.java))
            }
        } catch (e: Exception) {
            log.log(Level.SEVERE, "Fehler beim Verarbeiten des Transportergebnisses", e)
            throw RuntimeException(e)
        }
    }

    private fun handleConfirmed(msg: TransportConfirmed) {
        val order = em.find(StorageOrder::class.java, msg.storageOrderId) ?: return

        // Idempotenz: bereits abgeschlossen → still ignorieren
        if (order.status == StorageOrderStatus.COMPLETED) {
            log.info("Quittierung für StorageOrder ${msg.storageOrderId} bereits verarbeitet – übersprungen.")
            return
        }

        val location = em.find(StorageLocation::class.java, msg.targetLocationId) ?: return
        val carrier  = order.loadCarrier!!

        location.status  = LocationStatus.OCCUPIED
        carrier.location = location

        em.persist(StockMovement(
            loadCarrier    = carrier,
            article        = carrier.article!!,
            quantity       = carrier.quantity,
            targetLocation = location
        ))

        order.status = StorageOrderStatus.COMPLETED
        log.info("Bestandsbuchung für StorageOrder ${msg.storageOrderId} auf Platz ${location.id} abgeschlossen.")
    }

    private fun handleFaulted(msg: TransportFaulted) {
        val order    = em.find(StorageOrder::class.java, msg.storageOrderId) ?: return
        if (order.status == StorageOrderStatus.COMPLETED) return

        val faultedLocation = em.find(StorageLocation::class.java, msg.faultedLocationId) ?: return
        faultedLocation.status = LocationStatus.BLOCKED
        log.warning("Platz ${faultedLocation.id} gesperrt wegen Störung bei StorageOrder ${msg.storageOrderId}.")

        // Neuen Platz vergeben und erneut Transport anfordern
        val allLocations = em.createQuery("SELECT l FROM StorageLocation l", StorageLocation::class.java).resultList
        val newLocation  = slottingStrategy.reserveLocation(allLocations, order.loadCarrier!!.article!!.storageType)

        order.targetLocation = newLocation
        log.info("Neuer Zielplatz ${newLocation.id} für StorageOrder ${msg.storageOrderId} vergeben.")

        jmsSender.sendTransportRequested(TransportRequested(
            storageOrderId   = order.id,
            loadCarrierId    = order.loadCarrier!!.id,
            sourceLocationId = null,
            targetLocationId = newLocation.id
        ))
    }
}
