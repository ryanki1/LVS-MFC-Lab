package com.lvslab.lvs.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.Resource
import jakarta.ejb.Stateless
import jakarta.jms.JMSContext
import jakarta.jms.Queue
import jakarta.inject.Inject

@Stateless
class JmsSender {

    private val mapper = ObjectMapper()

    @Inject
    private lateinit var jmsContext: JMSContext

    @Resource(lookup = "java:/jms/queue/TransportRequests")
    private lateinit var transportRequests: Queue

    fun sendTransportRequested(message: TransportRequested) {
        val json = mapper.writeValueAsString(message)
        jmsContext.createProducer().send(transportRequests, json)
    }
}
