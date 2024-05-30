package de.smartsquare.starter.mqtt.metrics

import de.smartsquare.starter.mqtt.MqttConnector
import de.smartsquare.starter.mqtt.MqttHandler
import de.smartsquare.starter.mqtt.MqttMessageErrorHandler
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanPostProcessor

/**
 * Bean post processor that enables metrics for mqtt client by wrapping the handler and error handler.
 */
class MqttMetricsBeanPostProcessor(private val provider: ObjectProvider<MeterRegistry>) : BeanPostProcessor {
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any = when (bean) {
        is MqttConnector -> TimedMqttConnector(bean, provider.`object`)
        is MqttHandler -> MessageCountingMqttHandler(bean, provider.`object`)
        is MqttMessageErrorHandler -> ErrorCountingMqttMessageErrorHandler(bean, provider.`object`)
        else -> bean
    }
}
