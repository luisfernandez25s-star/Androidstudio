package com.example.aplicacionmovil

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class ChatActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var txtChat: TextView
    private lateinit var editMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnSaveMongo: Button
    private lateinit var btnShowMongo: Button
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
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
        // Mostrar diálogo de confirmación antes de guardar
        AlertDialog.Builder(this)
            .setTitle("Confirmar Guardado")
            .setMessage("¿Deseas guardar este mensaje en la base de datos?\n\n\"$mensaje\"")
            .setPositiveButton("Guardar") { _, _ ->
                val json = Gson().toJson(mapOf(
                    "path" to "/Escuela/Chat/Chat",
                    "usuario" to usuario,
                    "mensaje" to mensaje,
                    "fecha" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                ))
                val body = json.toRequestBody(JSON_TYPE)
                val request = Request.Builder().url("$API_URL/guardar").post(body).build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            AlertDialog.Builder(this@ChatActivity)
                                .setTitle("Error de Conexión")
                                .setMessage("No se pudo conectar con la base de datos:\n${e.message}")
                                .setPositiveButton("Entendido", null)
                                .show()
                            Log.e("ChatDebug", "Error de red al guardar", e)
                        }
                    }
                    override fun onResponse(call: Call, response: Response) {
                        val respBody = response.body?.string()
                        if (response.isSuccessful) {
                            runOnUiThread {
                                AlertDialog.Builder(this@ChatActivity)
                                    .setTitle("¡Guardado Exitoso!")
                                    .setMessage("El mensaje se ha guardado correctamente en la base de datos.")
                                    .setPositiveButton("OK", null)
                                    .show()
                                Log.d("ChatDebug", "Respuesta exitosa: $respBody")
                            }
                        } else {
                            runOnUiThread {
                                AlertDialog.Builder(this@ChatActivity)
                                    .setTitle("Error al Guardar")
                                    .setMessage("El servidor respondió con un error (${response.code}).\n\nDetalles: $respBody")
                                    .setPositiveButton("Cerrar", null)
                                    .show()
                                Log.e("ChatDebug", "Error del servidor al guardar: $respBody")
                            }
                        }
                    }
                })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun getFromMongo() {
        Toast.makeText(this, "Obteniendo datos...", Toast.LENGTH_SHORT).show()
        val request = Request.Builder().url("$API_URL/mensajes").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Error al conectar con el servidor", Toast.LENGTH_LONG).show()
                    Log.e("ChatDebug", "Error al obtener mensajes", e)
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    try {
                        val listType = object : TypeToken<List<Map<String, String>>>() {}.type
                        val mensajes: List<Map<String, String>> = Gson().fromJson(responseData, listType)
                        
                        runOnUiThread {
                            if (mensajes.isEmpty()) {
                                txtChat.append("\n[BD]: No hay mensajes guardados.")
                            } else {
                                txtChat.append("\n--- Mensajes en Base de Datos ---")
                                mensajes.forEach { m ->
                                    val u = m["usuario"] ?: "Anon"
                                    val msg = m["mensaje"] ?: ""
                                    val f = m["fecha"] ?: ""
                                    txtChat.append("\n($f) $u: $msg")
                                }
                                txtChat.append("\n--------------------------------")
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            txtChat.append("\n[Error parsing]: $responseData")
                            Log.e("ChatDebug", "Error al parsear JSON", e)
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Error del servidor: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(this)
    }
}
