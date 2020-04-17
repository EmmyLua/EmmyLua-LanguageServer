package com.tang.intellij.lua.plugin

import java.net.URLClassLoader

class PluginClassLoader(descriptor: PluginDescriptor) : URLClassLoader(arrayOf(descriptor.path.toURL())) {

}