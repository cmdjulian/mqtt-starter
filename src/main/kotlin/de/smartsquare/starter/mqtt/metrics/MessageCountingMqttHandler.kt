package de.smartsquare.starter.mqtt.metrics

import de.smartsquare.starter.mqtt.MqttHandler
import de.smartsquare.starter.mqtt.MqttPublishContainer
import io.micrometer.core.instrument.MeterRegistry

/**
 * Mqtt message counting client that counts the messages and delegates them to the given handler.
 */
class MessageCountingMqttHandler(delegate: MqttHandler, registry: MeterRegistry) : MqttHandler(delegate) {

    private val messageCount = registry.counter("mqtt.messages.count")
    private val messageSizeCount = registry.counter("mqtt.messages.bytes.count")

    override fun handle(message: MqttPublishContainer) {
        messageCount.increment()
        messageSizeCount.increment(message.payload.size.toDouble())
        super.handle(message)
    }
}
