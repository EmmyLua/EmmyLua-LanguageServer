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
    val port = System.getProperty("emmy.vscode.port")
    try {

        val serverSocket = AsynchronousServerSocketChannel.open().bind(InetSocketAddress("localhost", 5007))
        val threadPool = Executors.newCachedThreadPool()

        /*var inputStream = System.`in`
        var outputStream = System.out
        if (port != null) {
            val socket = Socket("localhost", Integer.parseInt(port))
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()
        }*/

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
    } catch (e: Exception) {
        System.err.println("EmmyLua language server failed to connect.")
        e.printStackTrace(System.err)
        System.exit(1)
    }
}