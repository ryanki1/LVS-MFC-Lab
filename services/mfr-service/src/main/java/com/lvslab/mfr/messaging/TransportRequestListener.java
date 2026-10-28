package com.lvslab.mfr.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lvslab.mfr.domain.TransportConfirmed;
import com.lvslab.mfr.domain.TransportFaulted;
import com.lvslab.mfr.domain.TransportOrder;
import com.lvslab.mfr.domain.TransportOrderStatus;
import com.lvslab.mfr.domain.TransportRequested;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.Queue;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup",  propertyValue = "java:/jms/queue/TransportRequests"),
    @ActivationConfigProperty(propertyName = "destinationType",    propertyValue = "jakarta.jms.Queue"),
    @ActivationConfigProperty(propertyName = "acknowledgeMode",    propertyValue = "Auto-acknowledge")
})
public class TransportRequestListener implements MessageListener {

    private static final Logger LOG = Logger.getLogger(TransportRequestListener.class.getName());

    private static final double FAULT_PROBABILITY = 0.1;

    private final ObjectMapper mapper = new ObjectMapper();

    @PersistenceContext(unitName = "MfrPU")
    private EntityManager em;

    @Resource(lookup = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(lookup = "java:/jms/queue/TransportResults")
    private Queue transportResults;

    @Override
    public void onMessage(Message message) {
        try {
            String body = message.getBody(String.class);
            TransportRequested request = mapper.readValue(body, TransportRequested.class);

            TransportOrder order = new TransportOrder(
                request.storageOrderId(),
                request.loadCarrierId(),
                request.sourceLocationId(),
                request.targetLocationId()
            );
            order.setStatus(TransportOrderStatus.SENT);
            em.persist(order);

            // Fördertechnik simulieren
            try (JMSContext ctx = connectionFactory.createContext()) {
                if (Math.random() < FAULT_PROBABILITY) {
                    LOG.info("Simulierte Störung für StorageOrder " + request.storageOrderId());
                    order.setStatus(TransportOrderStatus.FAULTED);
                    TransportFaulted faulted = new TransportFaulted(
                        request.storageOrderId(),
                        request.loadCarrierId(),
                        request.targetLocationId()
                    );
                    ctx.createProducer().send(transportResults, mapper.writeValueAsString(faulted));
                } else {
                    LOG.info("Quittierung für StorageOrder " + request.storageOrderId());
                    order.setStatus(TransportOrderStatus.CONFIRMED);
                    TransportConfirmed confirmed = new TransportConfirmed(
                        request.storageOrderId(),
                        request.loadCarrierId(),
                        request.targetLocationId()
                    );
                    ctx.createProducer().send(transportResults, mapper.writeValueAsString(confirmed));
                }
            }

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Fehler beim Verarbeiten der TransportRequested-Nachricht", e);
            // RuntimeException löst Rollback + Redelivery durch den Container aus
            throw new RuntimeException(e);
        }
    }
}
