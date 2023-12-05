package plugins.plugins

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.io.InputStreamReader


object TokenUtil {

    // 比如说LIGHT，DRAK下所有的key分别加上 _LIGHT _DRAK来区分
    enum class TokenType {
        LIGHT,
        DARK
    }

    // 表示颜色 color
    private enum class ColorKey(val type : String) {
        COLOR("color"),
        BG_COLOR("bg-color"),
        TEXT_COLOR("text-color"),
        BRAND_COLOR("brand-color"),
        LINK("link"),
        BORDER_COLOR("border-color")
    }

    // 这些key表示的是大小 dimen
    private enum class DimensKey(val type: String) {
        SIZE("size"),
        FONT_SIZE("font-size"),
        SPACING("spacing"),
        OPACITY("opacity"),
        FONT_WEIGHT("font-weight"),
        LINE_HEIGHT("line-height"),
        BORDER("border")
    }

    private enum class FontKey(val type : String) {
        // 字体的话，这里是不是需要提醒要把对应的字体，放到res/font文件夹下
        FONT_FAMILY("font-family")
    }

    // 目前不知道怎么处理的key
    private enum class OtherKey(val type : String) {
        BOXSHADOW("boxshadow")
    }

    // 这里表示的是需要组成一个style
    private enum class AliasStyleKey(val type : String) {
        TEXT_STYLE("text-style");

        companion object {
            fun tokenAttrToAndroidAttr(tokenAttr: String) : String {
                return when (tokenAttr) {
                    "fontFamily" -> "android:fontFamily"
                    "fontWeight" -> "android:textFontWeight"
                    // 这里的lineSpacingMultiplier是字体大小的倍数
                    "lineHeight" -> "android:lineSpacingMultiplier"
                    "fontSize" -> "android:textSize"
                    else -> ""
                }
            }

            // 这个方法，目前没有用到。因为好像Style无法嵌套style，那么style里的item里的值只能是直接的值？？
            fun tokenAttrToResPrefix(tokenAttr: String) : String {
                return when (tokenAttr) {
                    "fontFamily" -> "@font"
                    "fontWeight" -> "@dimen"
                    "lineHeight" -> "@dimen"
                    "fontSize" -> "@dimen"
                    else -> ""
                }
            }

            fun getTokenAttrMap(tokenAttr: String) : HashMap<String, String> {
                return when (tokenAttr) {
                    "fontFamily" -> fontsMap
                    "fontWeight" -> dimensMap
                    "lineHeight" -> dimensMap
                    "fontSize" -> dimensMap
                    else -> HashMap()
                }
            }
        }


    }

    // 目前还没有组件的token，但是如果有的话，组件最终产生的style，应该是多个style的组合，生成对应style的逻辑区别于上面的AliasStyleKey的逻辑。
    // 但是目前没找到如何嵌套style，只知道style能继承style，但是这种方式应该不对
    // 直接把style中的所有值写上也不是不行，我觉得。
    private enum class ComponentStyleKey(val type : String){}

    private val TAG = "LOG_TAG"

    private val ERRORMSG = "ERROR!!!"

    var colorsXMLFileAbsPath = ""

    var dimensXMLFileAbsPath = ""

    var aliasStyleXMLFileAbsPath = ""

    // 所有的颜色 这里同一放到这里 再统一插入res下面
    private var colorsMap : HashMap<String, String> = HashMap()
    // 所有的尺寸 比如间隔 字体大小 字体行高 等等
    private var dimensMap : HashMap<String, String> = HashMap()
    private var fontsMap : HashMap<String, String> = HashMap()
    // style里面是多个键值对
    private var aliasStyleMap : HashMap<String, HashMap<String, String>> = HashMap()

    private var curTokenType = TokenType.LIGHT

    fun parseToken(totalFileAbsPath : String, type : TokenType = TokenType.LIGHT) {
        curTokenType = type
        prepare(File(totalFileAbsPath).inputStream())
    }

    private fun prepare(inputStream: InputStream) {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val json = reader.use { it.readText() }
        val rawMap : LinkedTreeMap<String, Any> = Gson().fromJson(json, LinkedTreeMap::class.java) as LinkedTreeMap<String, Any>

        for ((outerKey, outerMap) in rawMap) {
            // 最外层的键值对中，可能有一些文件的附属信息，这里跳过这些信息
            if (outerKey in listOf("global", "Alias")) {
                mapToSpecialMap(outerMap as LinkedTreeMap<String, Any>)
            }
        }
    }

    // 解析map到具体的map如colorMap
    private fun mapToSpecialMap(map : LinkedTreeMap<String, Any>) {
        for ((key, value) in map) {
            when {
                key in ColorKey.values().map { it.type } -> {
                    initColorMap(value as LinkedTreeMap<String, LinkedTreeMap<String, Any>>, key.replace('-', '_'))
                }
                key in DimensKey.values().map { it.type } -> {
                    initDimenMap(value as LinkedTreeMap<String, LinkedTreeMap<String, Any>>, key.replace('-', '_'))
                }
                key in FontKey.values().map { it.type } -> {
                    initFontMap(value as LinkedTreeMap<String, LinkedTreeMap<String, Any>>, key.replace('-', '_'))
                }
                key in AliasStyleKey.values().map { it.type } -> {
                    initAliasStyleMap(value as LinkedTreeMap<String, LinkedTreeMap<String, Any>>, toCamelCase(key)) // key转换成驼峰命名
                }

            }
        }
    }

    private fun initColorMap(colorMapParams : LinkedTreeMap<String, LinkedTreeMap<String, Any>>?, prefix : String = "") {
        colorMapParams ?: return
        for ((key, value) in colorMapParams) {
            // 得到前缀
            val colorMapKey = prefix + "_" + key.replace("-", "_")
            // 有"value" 这个key 证明到了最后一层了
            if (value.containsKey("value"))
                colorsMap[colorMapKey] = getColorStr(value["value"] as String)
            else
                initColorMap(value as? LinkedTreeMap<String, LinkedTreeMap<String, Any>>?, colorMapKey)
        }
    }

    /*
    * 设计给的颜色值不是16进制的 需要自己转换
    * rgba(255,221,0,0.48) -> #7AFFDD00
    * */
    private fun getColorStr(rgbaColor: String?): String {
        rgbaColor ?: return ""
        // 判断对应颜色是否不能直接解析
        if (colorFromParentLevel(rgbaColor)) {
            // 这一种异常情况 目前不处理
            if (rgbaColor.startsWith("linear-gradient"))
                return ERRORMSG
            // 这里的颜色值，外面有一层{}。比如{color.brand100}。这种时候 我们应该把结果处理成 @color/brand100
            return "@color/" + getValueKey(rgbaColor)
        }

        if (rgbaColor.startsWith("#"))
            return rgbaColor

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

    // 除了这两种情况，都需要从已经解析好的颜色里去取
    private fun colorFromParentLevel(colorStr : String) = !(colorStr.startsWith("rgba(") || colorStr.startsWith("#"))

    // 比如{color.brand100}。这种时候 我们应该把结果处理成 color_brand100
    // 不止颜色，其他地方也会用到
    private fun getValueKey(originKey : String) =  originKey.substring(1, originKey.length-1).replace(".", "_").replace("-", "_")

    // 给我们的8位16进制颜色 透明度是反的需要改成Android可以使用的颜色
    // "#ff404000" -> "#004040ff"
//    private fun adjustRgba(colorStr : String) : String {
//        if (colorStr.length != 7)
//            return error
//        val charArray = colorStr.toCharArray() // 将字符串转换为 CharArray 数组
//        charArray[1] = colorStr[7]
//        charArray[2] = colorStr[8]
//        return String(charArray)
//    }

    // DimenMap目前不存在一个值引用其他值的情况
    private fun initDimenMap(dimenMapParams : LinkedTreeMap<String, LinkedTreeMap<String, Any>>?, prefix : String = "") {
        dimenMapParams ?: return
        for ((key, value) in dimenMapParams) {
            // 得到前缀
            val dimensMapKey = prefix + "_" + key.replace("-", "_")
            if (value.containsKey("value")) {
                val dimensStr = value["value"] as String
                if (couldBeApplyDirect(dimensStr))
                    dimensMap[dimensMapKey] = dimensStr
                else {
                    //  目前遇到的唯一情况就是 行高和字体大小相关 line-height:140%
                    try {
                        dimensMap[dimensMapKey] = (dimensStr.removeSuffix("%").toDouble() / 100).toString()
                    } catch (e : java.lang.Exception) {
                        dimensMap[dimensMapKey] = ERRORMSG
                    }

                }
            }
            else
                initDimenMap(value as? LinkedTreeMap<String, LinkedTreeMap<String, Any>>?, dimensMapKey)
        }
    }

    // Dimen里面很多都直接是数字 px等等 这些可以直接使用
    private fun couldBeApplyDirect(dimenStr : String) : Boolean{
        if (dimenStr.contains("px"))
            return true;
        try {
            dimenStr.toDouble()
            return true
        } catch (e : Exception){

        }finally {

        }

        return false
    }

    // fontMap目前只是保存在内存，能不能有啥办法，提醒我们需要把对应的字体放到res/font目录下
    private fun initFontMap(fontMapParams : LinkedTreeMap<String, LinkedTreeMap<String, Any>>?, prefix : String = "") {
        fontMapParams ?: return
        for ((key, value) in fontMapParams) {
            // 得到前缀
            val fontsMapKey = prefix + "_" + key.replace("-", "_")
            if (value.containsKey("value")) {
                val fontName = value["value"] as String
                // 比如 fontName = Gilroy
                fontsMap[fontsMapKey] = "@font/$fontName"
            }
            else
                initFontMap(value as? LinkedTreeMap<String, LinkedTreeMap<String, Any>>?, fontsMapKey)
        }
    }

    private fun initAliasStyleMap(aliasStyleMapParams : LinkedTreeMap<String, LinkedTreeMap<String, Any>>?, prefix : String = "") {
        aliasStyleMapParams ?: return
        for ((key, value) in aliasStyleMapParams) {
            var styleMapKey = prefix + toCamelCase(key)
            if (value.containsKey("value")) {
                // 举个例子，下面的styleMapKey textStyleDisplay32
                // 同时value["value"] 是 {fontFamily={font-family.body}, fontWeight={font-weight.regular}, lineHeight={line-height.regular}, fontSize={font-size.320}}
                styleMapKey =  styleMapKey[0] + styleMapKey.substring(1)
                aliasStyleMap[styleMapKey] = HashMap()
                for ((key, value) in value["value"] as LinkedTreeMap<String, String>) {
                    // key value 比如说是fontFamily {font-family.body}
                    val valueKey = getValueKey(value)
                    aliasStyleMap[styleMapKey]?.set(
                        AliasStyleKey.tokenAttrToAndroidAttr(key), AliasStyleKey.getTokenAttrMap(
                            key
                        )?.get(valueKey) ?: "")
                }
            }
            else
                initAliasStyleMap(value as? LinkedTreeMap<String, LinkedTreeMap<String, Any>>?, styleMapKey)
        }
    }

    // text-style -> textStyle 注意我下面的代码把首字母大写了，所以最后生成的名字第一个字母需要变小写
    private fun toCamelCase(originStr : String) : String{
//        val upperStr = originStr.replaceFirstChar { it.uppercaseChar() }
        val upperStr = originStr
        val regex = Regex("-(\\w)")
        return upperStr.replace(regex) {
            it.value[1].toUpperCase().toString()
        }
    }

    fun getAllFonts() = fontsMap.values.map{
        val regex = Regex("@font/")
        it.replace(regex, "")
    }.distinct()

    fun writeToXml() {
        if (colorsMap.size != 0)
            ColorsXMLParser.toXml(colorsXMLFileAbsPath, colorsMap)
        if (dimensMap.size != 0)
            DimenXMLParser.toXml(dimensXMLFileAbsPath, dimensMap)
        if (fontsMap.size != 0) {
            // 检测res/font目录下有没有对应字体？
        }
        if (aliasStyleMap.size != 0) {
            println(aliasStyleMap)
            AliasStyleXMLParser.toXml(aliasStyleXMLFileAbsPath, aliasStyleMap)
        }
    }


    // 把所有数值写到xml之后 应该要确保其他资源全部释放
//    fun destory() {
//        colorsMap.clear()
//        globalMap.clear()
//        aliasMap.clear()
//    }



    private object ColorsXMLParser {

        val beginString = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n"
        val endString = "</resources>\n"

        fun toXml(colorsXMLFileAbsPath : String, newColorsMap : HashMap<String, String>) {
            val writer = FileWriter(File(colorsXMLFileAbsPath))
            writer.write(beginString)
            for ((key, value) in newColorsMap) {
                writer.write("    <color ")
                writer.write("name=\"$key\">")
                writer.write("$value")
                writer.write("</color>\n")
            }
            writer.write(endString)
            writer.close()
        }

    }

    //    px转dp应该在哪一步
    private object DimenXMLParser {

        val beginString = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n"
        val endString = "</resources>\n"

        fun toXml(dimensXMLFileAbsPath: String, newDimensMap: HashMap<String, String>) {
            val writer = FileWriter(File(dimensXMLFileAbsPath))
            writer.write(beginString)
            for ((key, value) in newDimensMap) {
                writer.write("    <dimen ")
                writer.write("name=\"$key\">")
                writer.write("$value")
                writer.write("</dimen>\n")
            }
            writer.write(endString)

            writer.close()
        }
    }

    private object AliasStyleXMLParser {
        val beginString = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n"
        val endString = "</resources>\n"

        fun toXml(aliasStyleXMLFileAbsPath: String, aliasStyleMap: HashMap<String, HashMap<String, String>>) {
            val writer = FileWriter(File(aliasStyleXMLFileAbsPath))
            writer.write(beginString)
            for ((key, value) in aliasStyleMap) {
                writer.write("    <style name=\"")
                writer.write(key + "\">\n")

                for ((itemKey, itemValue) in value) {
                    writer.write("        <item name=\"")
                    writer.write(itemKey + "\">")
                    writer.write(itemValue)
                    writer.write("</item>\n")
                }

                writer.write("    </style>\n")
            }
            writer.write(endString)

            writer.close()
        }
    }

}