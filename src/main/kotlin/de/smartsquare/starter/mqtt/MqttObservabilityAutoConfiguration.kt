package de.smartsquare.starter.mqtt

import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass

/**
 * Autoconfiguration for mqtt observability which contributes metrics and tracing to the mqtt client.
 */
@ConditionalOnEnabledTracing
@ConditionalOnClass(HealthEndpoint::class)
@AutoConfiguration(after = [ObservationAutoConfiguration::class])
class MqttObservabilityAutoConfiguration
