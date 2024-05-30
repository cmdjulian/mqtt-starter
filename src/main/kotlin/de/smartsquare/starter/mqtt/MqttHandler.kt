package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.JsonMappingException
import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.datatypes.MqttTopicFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.util.ConcurrentLruCache
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction

/**
 * Class for consuming and forwarding messages to the correct subscriber.
 */
fun interface MqttHandler {
    /**
     * Handles a single [message]. The topic of the message is used to determine the correct subscriber which is then
     * invoked with parameters produced by the [MqttMessageAdapter].
     */
    fun handle(message: MqttPublishContainer)
}

/**
 * Class for consuming and forwarding messages to the correct subscriber.
 */
open class MqttHandlerImpl(
    private val collector: MqttSubscriberCollector,
    private val adapter: MqttMessageAdapter,
    private val messageErrorHandler: MqttMessageErrorHandler,
    subscriberTopicCacheSize: Int? = null,
) : MqttHandler {

    /**
     * Delegate for invoking a subscriber method.
     */
    private interface Subscriber {
        val parameterTypes: List<Class<*>>
        fun invoke(vararg args: Any)
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Cache for subscribers based on their topic. This is used to match incoming messages to the correct subscriber.
     * The cache is thread-safe and uses an LRU strategy.
     * The cache is initialized with a function that finds the correct subscriber for a given topic.
     * This is used to match wildcard topics to the correct subscriber by its topic filter.
     *
     * Example:
     * A subscriber registered for the topic "foo/+", after messages to "foo/bar" and "foo/baz" are received, the cache
     * will contain two entries for the subscriber with the topic filter "foo/+":
     * - "foo/bar" -> foo/+
     * - "foo/baz" -> foo/+
     *
     * This can then be used to find the correct subscriber for a given topic. In that way we prevent the need to
     * iterate over all subscribers for each message and create a delegate for each message topic. Especially for a
     * lot of different topics this can be a performance improvement.
     * The filter can be used to match the topic to the correct subscriber. via [subscriberCache]
     */
    private val topicToFilterCache = run {
        val cacheSize = if (subscriberTopicCacheSize == null) {
            collector.subscribers.size * 10
        } else {
            maxOf(subscriberTopicCacheSize, 0)
        }

        ConcurrentLruCache(cacheSize) { topic: MqttTopic ->
            collector.subscribers.find { sub -> sub.filter.matches(topic) }
                ?: error("No subscriber found for topic $topic")
        }
    }

    /**
     * Cache for subscribers based on their topic. This is used to match incoming messages to the correct subscriber.
     * We know that the cache will never grow larger than the amount of subscribers, so we can use a simple
     */
    private val subscriberCache = ConcurrentHashMap<MqttTopicFilter, Subscriber>(collector.subscribers.size)

    /**
     * Handles a single [message]. The topic of the message is used to determine the correct subscriber which is then
     * invoked with parameters produced by the [MqttMessageAdapter].
     */
    override fun handle(message: MqttPublishContainer) {
        logger.trace("Received mqtt message on topic [{}] with payload {}", message.topic, message.payload)

        val subscriber = getSubscriber(message.topic)

        try {
            val args = Array(subscriber.parameterTypes.size) { adapter.adapt(message, subscriber.parameterTypes[it]) }
            subscriber.invoke(*args)
        } catch (e: JsonMappingException) {
            messageErrorHandler.handle(
                MqttMessageException(
                    message.topic,
                    message.payload,
                    "Error while handling mqtt message on topic [${message.topic}]: Failed to map payload to target " +
                        "class",
                    e,
                ),
            )
        } catch (e: JacksonException) {
            messageErrorHandler.handle(
                MqttMessageException(
                    message.topic,
                    message.payload,
                    "Error while handling mqtt message on topic [${message.topic}]: Failed to parse payload",
                    e,
                ),
            )
        } catch (e: Exception) {
            messageErrorHandler.handle(
                MqttMessageException(
                    message.topic,
                    message.payload,
                    "Error while handling mqtt message on topic [${message.topic}]",
                    e,
                ),
            )
        }
    }

    protected fun invokeMethodHandle(handle: MethodHandle, bean: Any, vararg args: Any) {
        handle.invokeWithArguments(bean, *args)
    }

    protected fun invokeSuspendFunction(kFunction: KFunction<*>, bean: Any, vararg args: Any) {
        runBlocking(Dispatchers.Default) { kFunction.callSuspend(bean, *args) }
    }

    /**
     * Returns the subscriber for the given [topic].
     * If no subscriber is found, an error is thrown.
     * The subscriber is cached for performance reasons.
     *
     * If the function is a suspend function, it is wrapped in a suspend call. For normal functions, a method handle is
     * created and cached.
     */
    private fun getSubscriber(topic: MqttTopic): Subscriber {
        val subscriberInfo = topicToFilterCache[topic]

        return subscriberCache.computeIfAbsent(subscriberInfo.filter) { _ ->
            val kFunction = subscriberInfo.method.kotlinFunction
            val parameterTypes = kFunction?.valueParameters
                ?.map { it.type.jvmErasure.java }
                ?: subscriberInfo.method.parameterTypes.toList()

            return@computeIfAbsent if (kFunction?.isSuspend == true) {
                object : Subscriber {
                    override val parameterTypes: List<Class<*>> = parameterTypes
                    override fun invoke(vararg args: Any) = invokeSuspendFunction(kFunction, subscriberInfo.bean, *args)
                }
            } else {
                val handle = MethodHandles.publicLookup().unreflect(subscriberInfo.method)
                object : Subscriber {
                    override val parameterTypes: List<Class<*>> = parameterTypes
                    override fun invoke(vararg args: Any) = invokeMethodHandle(handle, subscriberInfo.bean, *args)
                }
            }
        }
    }
}
