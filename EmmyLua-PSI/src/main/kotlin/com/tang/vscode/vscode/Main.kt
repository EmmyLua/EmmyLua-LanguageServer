package com.tang.vscode.vscode

import org.eclipse.lsp4j.jsonrpc.Launcher
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.Channels
import java.util.concurrent.Executors


/**
 * tangzx
 * Created by Client on 2018/3/20.
 */
fun main(args: Array<String>) {
    val port = System.getProperty("emmy.port")
    try {
        if (port != null) startSocketServer(port.toInt())
        else start()
    } catch (e: Exception) {
        e.printStackTrace(System.err)
        System.exit(1)
    }
}

private fun start() {
    val inputStream = System.`in`
    val outputStream = System.out
    try {
        val server = LuaLanguageServer()
        val launcher = Launcher.createLauncher(server, LuaLanguageClient::class.java, inputStream, outputStream)
        server.connect(launcher.remoteProxy)
        launcher.startListening()
    } catch (e: Exception) {
        e.printStackTrace(System.err)
    }
}

private fun startSocketServer(port: Int) {
    val serverSocket = AsynchronousServerSocketChannel.open().bind(InetSocketAddress("localhost", port))
    val threadPool = Executors.newCachedThreadPool()

    while (true) {
        val socketChannel = serverSocket.accept().get()
        threadPool.execute {
            val inputStream = Channels.newInputStream(socketChannel)
            val outputStream = Channels.newOutputStream(socketChannel)

            try {
                val server = LuaLanguageServer()
                val launcher = Launcher.createLauncher(server, LuaLanguageClient::class.java, inputStream, outputStream)
                server.connect(launcher.remoteProxy)
                launcher.startListening()
            } catch (e: Exception) {
                socketChannel.close()
                e.printStackTrace(System.err)
            }
        }
    }
}