package de.smartsquare.starter.mqtt.metrics

import de.smartsquare.starter.mqtt.MqttMessageErrorHandler
import de.smartsquare.starter.mqtt.MqttMessageException
import io.micrometer.core.instrument.MeterRegistry

/**
 * Mqtt message error handler that counts the errors and logs them.
 */
class ErrorCountingMqttMessageErrorHandler(private val delegate: MqttMessageErrorHandler, registry: MeterRegistry) :
    MqttMessageErrorHandler() {

    private val errorCount = registry.counter("mqtt.errors.count")

    override fun handle(error: MqttMessageException) {
        errorCount.increment()
        delegate.handle(error)
    }
}
