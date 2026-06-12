package com.example.aplicacionmovil.presentation

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.aplicacionmovil.R
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

class ChatActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var txtChat: TextView
    private lateinit var editMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var scrollChat: ScrollView
    private val pathChat = "/chat"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_reloj)

        txtChat = findViewById(R.id.txtChat)
        editMessage = findViewById(R.id.editMessage)
        btnSend = findViewById(R.id.btnSend)
        scrollChat = findViewById(R.id.scrollChat)

        btnSend.setOnClickListener {
            val msg = editMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                appendChat("Reloj: $msg")
                sendToPhone(msg)
                editMessage.text.clear()
            }
        }
    }

    private fun sendToPhone(message: String) {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    appendChat("Sistema: no hay celular conectado")
                    return@addOnSuccessListener
                }

                nodes.forEach { node ->
                    Wearable.getMessageClient(this)
                        .sendMessage(node.id, pathChat, message.toByteArray(Charsets.UTF_8))
                        .addOnSuccessListener {
                            Log.d("ChatWear", "Mensaje enviado a ${node.displayName}: $message")
                        }
                        .addOnFailureListener { e ->
                            appendChat("Error enviando: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                appendChat("Error buscando celular: ${e.message}")
            }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == pathChat) {
            val msg = String(event.data, Charsets.UTF_8)
            runOnUiThread { appendChat("Celular: $msg") }
        }
    }

    private fun appendChat(text: String) {
        txtChat.append("\n$text")
        scrollChat.post { scrollChat.fullScroll(ScrollView.FOCUS_DOWN) }
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
