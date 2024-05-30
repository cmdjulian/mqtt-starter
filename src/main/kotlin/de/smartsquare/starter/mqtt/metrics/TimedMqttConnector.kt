package de.smartsquare.starter.mqtt.metrics

import de.smartsquare.starter.mqtt.MqttConnector
import io.micrometer.core.instrument.MeterRegistry

/**
 * Timed mqtt connector that records the initial connection time.
 */
class TimedMqttConnector(private val delegate: MqttConnector, registry: MeterRegistry) : MqttConnector() {

    private val connectionTimer = registry.timer("mqtt.connect.time")

    override fun stop(callback: Runnable) = delegate.stop(callback)

    override fun start() {
        connectionTimer.record(Runnable(delegate::start))
    }

    override fun isRunning(): Boolean = delegate.isRunning
}
