package com.boodschappen.app.data.remote

import com.boodschappen.app.data.local.ShoppingItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private data class ShoppingItemDto(
    val id: Long = 0,
    val name: String = "",
    val quantity: String = "1",
    val unit: String = "",
    val category: String = "Overig",
    val barcode: String? = null,
    val imageUrl: String? = null,
    val brand: String? = null,
    val isChecked: Boolean = false,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

private fun ShoppingItem.toDto() = ShoppingItemDto(
    id, name, quantity, unit, category, barcode, imageUrl, brand, isChecked, note, createdAt
)

private fun ShoppingItemDto.toEntity() = ShoppingItem(
    id = id, name = name, quantity = quantity, unit = unit,
    category = category,
    barcode = barcode?.takeIf { it.isNotBlank() },
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    brand = brand?.takeIf { it.isNotBlank() },
    isChecked = isChecked, note = note, createdAt = createdAt
)

class MqttSyncRepository {

    private val gson = Gson()
    private var mqttClient: MqttClient? = null

    companion object {
        private const val BROKER_URI = "tcp://broker.hivemq.com:1883"
        private const val TOPIC_PREFIX = "boodschappen/v1/"

        fun generateCode(): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            return (1..6).map { chars.random() }.joinToString("")
        }

        fun newItemId(): Long =
            abs(UUID.randomUUID().hashCode().toLong()).let { if (it == 0L) 1L else it }
    }

    private fun topic(code: String) = "$TOPIC_PREFIX$code"

    private suspend fun getClient(): MqttClient = withContext(Dispatchers.IO) {
        val existing = mqttClient
        if (existing != null && existing.isConnected) return@withContext existing
        val client = MqttClient(BROKER_URI, "boodschappen-${UUID.randomUUID()}", MemoryPersistence())
        val opts = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 15
            keepAliveInterval = 30
        }
        client.connect(opts)
        mqttClient = client
        client
    }

    fun getItemsFlow(listCode: String): Flow<List<ShoppingItem>> = callbackFlow {
        val client = getClient()
        val t = topic(listCode)

        client.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) = Unit
            override fun deliveryComplete(token: IMqttDeliveryToken) = Unit
            override fun messageArrived(topic: String, message: MqttMessage) {
                trySend(parseItems(String(message.payload)))
            }
        })
        client.subscribe(t, 1)

        awaitClose {
            runCatching { client.unsubscribe(t) }
        }
    }

    suspend fun publishList(listCode: String, items: List<ShoppingItem>) =
        withContext(Dispatchers.IO) {
            val client = getClient()
            val json = gson.toJson(items.map { it.toDto() })
            val msg = MqttMessage(json.toByteArray()).apply {
                qos = 1
                isRetained = true
            }
            client.publish(topic(listCode), msg)
        }

    suspend fun listExists(listCode: String): Boolean = withContext(Dispatchers.IO) {
        val client = getClient()
        val t = topic(listCode)
        var exists = false
        val latch = CountDownLatch(1)

        client.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) = Unit
            override fun deliveryComplete(token: IMqttDeliveryToken) = Unit
            override fun messageArrived(topic: String, message: MqttMessage) {
                if (message.payload.isNotEmpty()) { exists = true; latch.countDown() }
            }
        })
        client.subscribe(t, 1)
        latch.await(3, TimeUnit.SECONDS)
        runCatching { client.unsubscribe(t) }
        client.setCallback(null)
        exists
    }

    fun disconnect() {
        runCatching { mqttClient?.disconnect() }
        mqttClient = null
    }

    private fun parseItems(json: String): List<ShoppingItem> = try {
        val type = object : TypeToken<List<ShoppingItemDto>>() {}.type
        val dtos: List<ShoppingItemDto> = gson.fromJson(json, type) ?: emptyList()
        dtos.map { it.toEntity() }.sortedWith(compareBy({ it.isChecked }, { -it.createdAt }))
    } catch (_: Exception) { emptyList() }
}
