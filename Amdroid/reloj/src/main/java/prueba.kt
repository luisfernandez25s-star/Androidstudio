package com.example.aplicacionmovil.presentation

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.aplicacionmovil.R
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlin.concurrent.thread

class Prueba : Activity(), MessageClient.OnMessageReceivedListener {

    private lateinit var txtChat: TextView
    private lateinit var editMessage: EditText
    private lateinit var btnSend: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_reloj) // Usamos el layout del chat que ya tiene ScrollView

        txtChat = findViewById(R.id.txtChat)
        editMessage = findViewById(R.id.editMessage)
        btnSend = findViewById(R.id.btnSend)

        Wearable.getMessageClient(this).addListener(this)

        btnSend.setOnClickListener {
            val msg = editMessage.text.toString()
            if (msg.isNotEmpty()) {
                sendMessageToPhone(msg)
                txtChat.append("\nTú: $msg")
                editMessage.text.clear()
            }
        }
    }

    private fun sendMessageToPhone(message: String) {
        thread {
            try {
                val nodes = Tasks.await(Wearable.getNodeClient(this).connectedNodes)
                for (node in nodes) {
                    Wearable.getMessageClient(this).sendMessage(node.id, "/chat", message.toByteArray())
                }
            } catch (e: Exception) {
                Log.e("ChatDebug", "Error enviando desde reloj", e)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/chat") {
            val msg = String(messageEvent.data)
            runOnUiThread {
                txtChat.append("\nCel: $msg")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(this)
    }
}
