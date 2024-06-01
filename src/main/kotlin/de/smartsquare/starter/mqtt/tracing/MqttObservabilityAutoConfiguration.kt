package de.smartsquare.starter.mqtt.tracing

import de.smartsquare.starter.mqtt.MqttAutoConfiguration
import de.smartsquare.starter.mqtt.MqttHandler
import de.smartsquare.starter.mqtt.MqttMessageAdapter
import de.smartsquare.starter.mqtt.MqttMessageErrorHandler
import de.smartsquare.starter.mqtt.MqttSubscriberCollector
import io.micrometer.observation.ObservationRegistry
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean

/**
 * Autoconfiguration for mqtt observability which contributes metrics and tracing to the mqtt client.
 */
@ConditionalOnEnabledTracing
@ConditionalOnClass(ObservationRegistry::class)
@AutoConfiguration(after = [ObservationAutoConfiguration::class, MqttAutoConfiguration::class])
class MqttObservabilityAutoConfiguration {
    @Bean
    @ConditionalOnBean(
        ObservationRegistry::class,
        MqttSubscriberCollector::class,
        MqttMessageAdapter::class,
        MqttMessageErrorHandler::class,
    )
    fun observingMqttHandler(
        registry: ObservationRegistry,
        collector: MqttSubscriberCollector,
        adapter: MqttMessageAdapter,
        messageErrorHandler: MqttMessageErrorHandler,
    ): MqttHandler {
        TODO(registry.toString() + collector.toString() + adapter.toString() + messageErrorHandler.toString())
    }
}
