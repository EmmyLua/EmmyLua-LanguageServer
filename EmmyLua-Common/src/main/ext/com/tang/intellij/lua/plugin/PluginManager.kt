package com.tang.intellij.lua.plugin

import org.w3c.dom.Node
import java.io.File
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory


data class PluginDescriptor(
        val name: String,
        val path: URI
) {
    var classLoader: ClassLoader? = null
}

object PluginManager {

    private val plugins = mutableMapOf<String, PluginDescriptor>()
    private val coreClassLoader = javaClass.classLoader
    private var corePluginDescriptor: PluginDescriptor? = null
    private val extensionArea = ExtensionArea()

    fun init() {
        loadCorePlugin()
    }

    fun loadPlugin(path: String) {

    }

    fun loadPlugin(name: String, path: String): PluginDescriptor? {
        val uri = File(path).toURI()
        val descriptor = PluginDescriptor(name, uri)
        initClassLoader(descriptor)
        plugins[name] = descriptor
        descriptor.classLoader?.let {
            loadPluginXML(it, descriptor)
        }
        return descriptor
    }

    fun <T> getExtensionPoint(sQualifiedName: String): ExtensionPoint<T>? {
        return extensionArea.getExtensionPoint(sQualifiedName)
    }

    private fun initClassLoader(descriptor: PluginDescriptor) {
        val loader = PluginClassLoader(descriptor)
        descriptor.classLoader = loader
    }

    private fun loadCorePlugin() {
        val location = javaClass.protectionDomain.codeSource.location
        val descriptor = PluginDescriptor("core", location.toURI())
        descriptor.classLoader = coreClassLoader
        corePluginDescriptor = descriptor
        loadPluginXML(coreClassLoader, descriptor)
    }

    private fun loadPluginXML(loader: ClassLoader, descriptor: PluginDescriptor) {
        val area = extensionArea
        val pluginXML = loader.getResourceAsStream("plugin.xml")
        if (pluginXML != null) {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pluginXML)
            val root = doc.childNodes.item(0)
            for (i in 0 until root.childNodes.length) {
                val c = root.childNodes.item(i)
                when (c.nodeName) {
                    "extensionPoints" -> readExtensionPoints(c, area, descriptor)
                    "extensions" -> readExtensions(c, area, descriptor)
                    else -> {
                    }
                }
            }
        }
    }

    private fun readExtensionPoints(node: Node, area: ExtensionArea, descriptor: PluginDescriptor) {
        for (i in 0 until node.childNodes.length) {
            val n = node.childNodes.item(i)
            if (n.nodeName == "extensionPoint") {
                val sQualifiedName = n.getAttribute("qualifiedName")
                val sInterface = n.getAttribute("interface")
                if (sQualifiedName != null && sInterface != null) {
                    area.registerExtensionPoint(ExtensionPoint<Any>(sQualifiedName, sInterface))
                }
            }
        }
    }

    private fun readExtensions(node: Node, area: ExtensionArea, descriptor: PluginDescriptor) {
        val defaultNS = node.getAttribute("defaultExtensionNs")
        for (i in 0 until node.childNodes.length) {
            val n = node.childNodes.item(i)
            val extName = if (defaultNS == null) n.nodeName else "${defaultNS}.${n.nodeName}"
            val extPoint = area.getExtensionPoint<Any>(extName)
            val implementation = n.getAttribute("implementation")
            if (extPoint != null && implementation != null) {
                val adapter = XmlExtensionAdapter(descriptor, implementation)
                extPoint.addExtension(adapter)
            }
        }
    }

    private fun Node.getAttribute(name: String): String? {
        val item = attributes?.getNamedItem(name)
        return item?.textContent
    }
}