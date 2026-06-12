package com.example.aplicacionmovil

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var txtChat: TextView
    private lateinit var editMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnSave: Button
    private lateinit var btnShow: Button
    private lateinit var scrollChat: ScrollView

    private val pathChat = "/chat"
    private val apiGuardar = "https://appmovil-2gf6.onrender.com/guardar"
    private val client = OkHttpClient()
    private var lastMessage = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        txtChat = findViewById(R.id.txtChat)
        editMessage = findViewById(R.id.editMessage)
        btnSend = findViewById(R.id.btnSend)
        btnSave = findViewById(R.id.btnSave)
        btnShow = findViewById(R.id.btnShow)
        scrollChat = findViewById(R.id.scrollChat)

        btnSend.setOnClickListener {
            val msg = editMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                lastMessage = msg
                appendChat("Celular: $msg")
                sendToWatch(msg)
                editMessage.text.clear()
            }
        }

        btnSave.setOnClickListener {
            val msg = if (editMessage.text.toString().trim().isNotEmpty()) {
                editMessage.text.toString().trim()
            } else {
                lastMessage
            }

            if (msg.isNotEmpty()) {
                guardarEnBaseDeDatos(msg)
            } else {
                appendChat("Sistema: escribe o recibe un mensaje antes de guardar")
            }
        }

        btnShow.setOnClickListener {
            val msg = editMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                lastMessage = msg
                appendChat("Vista: $msg")
                editMessage.text.clear()
            } else if (lastMessage.isNotEmpty()) {
                appendChat("Último mensaje: $lastMessage")
            } else {
                appendChat("Sistema: no hay mensaje para mostrar")
            }
        }
    }

    private fun sendToWatch(message: String) {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    appendChat("Sistema: no hay reloj conectado")
                    return@addOnSuccessListener
                }

                nodes.forEach { node ->
                    Wearable.getMessageClient(this)
                        .sendMessage(node.id, pathChat, message.toByteArray(Charsets.UTF_8))
                        .addOnSuccessListener {
                            Log.d("ChatMobile", "Mensaje enviado a ${node.displayName}: $message")
                        }
                        .addOnFailureListener { e ->
                            appendChat("Error enviando al reloj: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                appendChat("Error buscando reloj: ${e.message}")
            }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == pathChat) {
            val msg = String(event.data, Charsets.UTF_8)
            lastMessage = msg
            runOnUiThread {
                appendChat("Reloj: $msg")
            }
        }
    }

    private fun guardarEnBaseDeDatos(message: String) {
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val json = """
            {
              "usuario": "Celular",
              "mensaje": "${escapeJson(message)}",
              "fecha": "$fecha"
            }
        """.trimIndent()

        post(apiGuardar, json)
    }

    private fun get(url: String) {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { appendChat("GET error: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string().orEmpty()
                runOnUiThread { appendChat("GET respuesta: $body") }
            }
        })
    }

    private fun post(url: String, jsonBody: String) {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { appendChat("Error al guardar: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string().orEmpty()
                runOnUiThread {
                    if (response.isSuccessful) {
                        appendChat("Guardado en MongoDB: $body")
                    } else {
                        appendChat("Servidor respondió ${response.code}: $body")
                    }
                }
            }
        })
    }

    private fun appendChat(text: String) {
        txtChat.append("\n$text")
        scrollChat.post { scrollChat.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun escapeJson(text: String): String {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
    }
}
