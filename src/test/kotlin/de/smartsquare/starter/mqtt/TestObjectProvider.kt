package de.smartsquare.starter.mqtt

import org.springframework.beans.factory.ObjectProvider

class TestObjectProvider<T : Any>(private val data: T) : ObjectProvider<T> {
    override fun getObject(vararg args: Any?): T = data
    override fun getObject(): T = data
    override fun getIfAvailable(): T = data
    override fun getIfUnique(): T = getObject()
}
