package util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

// sizing fontSizes color
object TokenUtil {

    private val TAG = "sxd"

    private enum class TokenType {
        GLOBAL,
        ALIAS,
        COMPONENT
    }

    private var globalMap : LinkedTreeMap<String, Any> = LinkedTreeMap()
    private var aliasMap : LinkedTreeMap<String, Any> = LinkedTreeMap()
    private var componentMap : LinkedTreeMap<String, Any> = LinkedTreeMap()

    // 一级token 二级token里都会有颜色 这里同一放到这里 再统一插入res下面的color.xml
    private var colorMap : LinkedTreeMap<String, String> = LinkedTreeMap()
    private var sizingMap : LinkedTreeMap<String, String> = LinkedTreeMap()
    private var fontSizesMap : LinkedTreeMap<String, String> = LinkedTreeMap()

    fun update(globalTokenInputStreamParams: InputStream? = null, aliasTokenInputStream: InputStream? = null,
               componentTokenInputStream: InputStream? = null) {
        globalTokenInputStreamParams?.let {
            inputStreamToMap(it, TokenType.GLOBAL)
            mapToSpecialMap(globalMap)
        }
        aliasTokenInputStream?.let {
            inputStreamToMap(it, TokenType.ALIAS)
            mapToSpecialMap(aliasMap)
        }
        componentTokenInputStream?.let {
            inputStreamToMap(it, TokenType.COMPONENT)
            mapToSpecialMap(componentMap)
        }

        Log.e(TAG, colorMap.toString())
    }

    // 把json文件流转换成map
    private fun inputStreamToMap(inputStream: InputStream, tokenType: TokenType) {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val json = reader.use { it.readText() }
        val map : LinkedTreeMap<String, Any> = Gson().fromJson(json, LinkedTreeMap::class.java) as LinkedTreeMap<String, Any>
        when (tokenType) {
            TokenType.GLOBAL -> globalMap = map
            TokenType.ALIAS -> aliasMap = map
            TokenType.COMPONENT -> componentMap = map
        }
    }

    // 解析map到具体的map如colorMap
    private fun mapToSpecialMap(map : LinkedTreeMap<String, Any>) {
        for ((key, value) in map) {
            when (key) {
                "color" -> {
                    initColorMap(value as LinkedTreeMap<String, LinkedTreeMap<String, Any>>)
                }
                else -> {}
            }
        }
    }

    private fun initColorMap(colorMapParams : LinkedTreeMap<String, LinkedTreeMap<String, Any>>?, prefix : String = "") {
        colorMapParams ?: return
        for ((key, value) in colorMapParams) {
            // 得到前缀
            val colorMapKey = prefix + key.replace("-", "_") + "_"
            // 有"value" 这个key 证明到了最后一层了
            if (value.containsKey("value"))
                colorMap[colorMapKey.substring(0, colorMapKey.length-1)] = rgbaToHex8(value["value"] as String)
            else
                initColorMap(value as? LinkedTreeMap<String, LinkedTreeMap<String, Any>>?, colorMapKey)
        }
    }

    /*
    * 设计给的颜色值不是16进制的 需要自己转换
    * rgba(255,221,0,0.48) -> #7AFFDD00
    * */
    private fun rgbaToHex8(rgbaColor: String?): String {
        rgbaColor ?: return ""
        // 判断对应颜色是否不能直接解析 例如{color.brand100} 那么这种就是已经解析好了 并且不是rgba无法直接解析
        if (rgbaColor.startsWith("{color.")) {
            // 细节之后处理 这里先随便返回一个值
            return colorMap[rgbaColor.substring(7, rgbaColor.length-1).replace(".", "_")] ?: "#000000"
        }
        val colorComponents = rgbaColor
            .substring(rgbaColor.indexOf("(") + 1, rgbaColor.indexOf(")"))
            .split(",")
            .map { it.trim().toDouble() }

        val red = colorComponents[0].toInt()
        val green = colorComponents[1].toInt()
        val blue = colorComponents[2].toInt()

        // 将 alpha 值乘以 255 并四舍五入取整
        val alpha = (colorComponents[3] * 255 + 0.5).toInt()
        val hexColor = Integer.toHexString((alpha shl 24) or (red shl 16) or (green shl 8) or blue).toUpperCase()

        return "#${hexColor}"
    }

    // 把所有数值写到xml之后 应该要确保其他资源全部释放
    fun destory() {
        colorMap.clear()
        globalMap.clear()
        aliasMap.clear()
    }
}

// 特定于解析token用的xml解析器 目的是更新xml文件
//private object XMLUtil {
//    //  目前来说不知道这些东西能不能通用 先写一个特例看看
//    fun updateColorXML (path : String) {
//        val saxParserFactory = SAXParserFactory.newInstance()
//        try {
//            val parser = saxParserFactory.newSAXParser()
//            parser.parse(file, object : DefaultHandler(){
//
//            }
//        }catch(e : Exception) {
//
//        }
//    }
//}