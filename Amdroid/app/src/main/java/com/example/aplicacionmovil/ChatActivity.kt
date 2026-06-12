package com.example.aplicacionmovil

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.concurrent.thread

class ChatActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var txtChat: TextView
    private lateinit var editMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnSaveMongo: Button
    private lateinit var btnShowMongo: Button
    
    private val client = OkHttpClient()
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    
    // URL DE RENDER ACTUALIZADA
    private val API_URL = "https://chat-reloj-backend.onrender.com" 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        txtChat = findViewById(R.id.txtChat)
        editMessage = findViewById(R.id.editMessage)
        btnSend = findViewById(R.id.btnSend)
        btnSaveMongo = findViewById(R.id.btnSaveMongo)
        btnShowMongo = findViewById(R.id.btnShowMongo)

        // Escuchar mensajes del reloj mediante MessageClient
        Wearable.getMessageClient(this).addListener(this)

        // Botón ENVIAR: Manda mensaje al reloj
        btnSend.setOnClickListener {
            val msg = editMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessageToWatch(msg)
                txtChat.append("\nTú: $msg")
                editMessage.text.clear()
            }
        }

        // Botón AGREGAR A BASE DE DATOS: Envía a MongoDB vía API
        btnSaveMongo.setOnClickListener {
            val msg = editMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                postToMongo("Luis", msg)
            } else {
                Toast.makeText(this, "Escribe algo en el chat para guardar", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón MOSTRAR LO GUARDADO: Obtiene datos de MongoDB
        btnShowMongo.setOnClickListener {
            getFromMongo()
        }
    }

    private fun sendMessageToWatch(message: String) {
        thread {
            try {
                val nodes = Tasks.await(Wearable.getNodeClient(this).connectedNodes)
                if (nodes.isEmpty()) {
                    runOnUiThread { Toast.makeText(this, "No hay reloj conectado", Toast.LENGTH_SHORT).show() }
                }
                for (node in nodes) {
                    Wearable.getMessageClient(this).sendMessage(node.id, "/chat", message.toByteArray())
                }
            } catch (e: Exception) {
                Log.e("ChatDebug", "Error enviando al reloj", e)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/chat") {
            val msg = String(messageEvent.data)
            runOnUiThread {
                txtChat.append("\nReloj: $msg")
            }
        }
    }

    private fun postToMongo(usuario: String, mensaje: String) {
        val json = Gson().toJson(mapOf(
            "usuario" to usuario,
            "mensaje" to mensaje,
            "fecha" to System.currentTimeMillis().toString()
        ))
        val body = json.toRequestBody(JSON_TYPE)
        val request = Request.Builder().url("$API_URL/guardar").post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread { Toast.makeText(applicationContext, "¡Mensaje guardado en MongoDB!", Toast.LENGTH_SHORT).show() }
                } else {
                    runOnUiThread { Toast.makeText(applicationContext, "Error del servidor: ${response.code}", Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }

    private fun getFromMongo() {
        val request = Request.Builder().url("$API_URL/mensajes").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "Error al obtener datos", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    txtChat.append("\n[BD Mongo]: $responseData")
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(this)
    }
}
