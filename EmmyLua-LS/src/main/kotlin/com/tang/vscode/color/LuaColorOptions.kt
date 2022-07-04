//package com.tang.vscode.color
//
//import org.eclipse.lsp4j.Color
//import java.lang.Integer.*
//
//object LuaColorOptions {
//    var docTypeColor = makeColor("#66CCFF")
//
//    var globalColor = makeColor("#FF6699")
//
//    var parameterColor = makeColor("#9FFC")
//
//    var upvalueColor = makeColor("#9FFC")
//
//    fun makeColor(colorString: String): Color {
//        val color = Color()
//        if (colorString.startsWith('#')) {
//            val colorDetail = colorString.substring(1)
//            var i = 0
//            var step = min(2, colorDetail.length - i)
//            if(step != 0) {
//                color.red = parseInt(colorDetail.substring(i, i + step), 16).toDouble() / 255
//                i += step
//            }
//            step = min(2, colorDetail.length - i)
//            if(step != 0) {
//                color.green = parseInt(colorDetail.substring(i, i + 2), 16).toDouble() / 255
//                i += step
//            }
//            step = min(2, colorDetail.length - i)
//            if(step != 0) {
//                color.blue = parseInt(colorDetail.substring(i, i + 2), 16).toDouble() / 255
//                i += 2
//            }
//        }
//
//        return color
//    }
//}