package de.smartsquare.starter.mqtt.metrics

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource
import de.smartsquare.starter.mqtt.Mqtt3ClientConfigurer
import de.smartsquare.starter.mqtt.Mqtt5ClientConfigurer
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

/**
 * Autoconfiguration for mqtt observability which contributes metrics to the mqtt client.
 */
@ConditionalOnClass(MeterRegistry::class)
@ConditionalOnBean(MeterRegistry::class, MqttClient::class)
@Import(MqttMetricsBeanPostProcessor::class)
@AutoConfiguration(
    after = [
        MetricsAutoConfiguration::class,
        CompositeMeterRegistryAutoConfiguration::class,
        MqttMetricsAutoConfiguration::class,
    ],
)
class MqttMetricsAutoConfiguration {
    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "3")
    fun mqtt3ConnectionErrorCounter(registry: MeterRegistry) = Mqtt3ClientConfigurer { builder ->
        builder.addDisconnectedListener { context ->
            if (context.source != MqttDisconnectSource.USER) {
                registry.counter("mqtt.disconnect.count").increment()
            }
        }
    }

    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "5")
    fun mqtt5ConnectionErrorCounter(registry: MeterRegistry) = Mqtt5ClientConfigurer { builder ->
        builder.addDisconnectedListener { context ->
            if (context.source != MqttDisconnectSource.USER) {
                registry.counter("mqtt.disconnect.count").increment()
            }
        }
    }
}
