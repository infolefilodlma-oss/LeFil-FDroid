package net.lefil.fdroid

import android.util.Log
import java.net.ServerSocket

/**
 * Serveur de test éphémère : existe uniquement pendant la durée du test
 * instrumenté, dans le processus de test, sur un port local aléatoire.
 * Répond une seule fois avec le corps fourni, puis se referme. Aucune
 * donnée personnelle, aucune persistance, aucun serveur de production.
 */
class FakeEventServer(private val responseBody: String) {

    private var serverSocket: ServerSocket? = null

    @Volatile
    private var running = false

    private var thread: Thread? = null

    var port: Int = 0
        private set

    fun start() {
        val socket = ServerSocket(0)
        serverSocket = socket
        port = socket.localPort
        running = true

        thread = Thread {
            while (running) {
                try {
                    socket.accept().use { client ->
                        client.getInputStream().bufferedReader().readLine() // consomme la requête, ignorée
                        val body = responseBody.toByteArray()
                        val header = "HTTP/1.1 200 OK\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                        client.getOutputStream().apply {
                            write(header.toByteArray())
                            write(body)
                            flush()
                        }
                        Log.i("LeFilTestServer", "Évènement émis : $responseBody (port $port)")
                    }
                } catch (e: Exception) {
                    if (running) {
                        Log.w("LeFilTestServer", "Serveur de test arrêté ou erreur : ${e.message}")
                    }
                }
            }
        }
        thread?.start()
    }

    fun stop() {
        running = false
        runCatching { serverSocket?.close() }
    }
}
