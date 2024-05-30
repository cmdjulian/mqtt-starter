package de.smartsquare.starter.mqtt.tracing

import de.smartsquare.starter.mqtt.MqttAutoConfiguration
import io.micrometer.observation.ObservationRegistry
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean

/**
 * Autoconfiguration for mqtt observability which contributes metrics and tracing to the mqtt client.
 */
@ConditionalOnEnabledTracing
@ConditionalOnBean(ObservationRegistry::class)
@AutoConfiguration(after = [ObservationAutoConfiguration::class, MqttAutoConfiguration::class])
class MqttObservabilityAutoConfiguration
