package plugins.plugins

/*
TODO: 整个token解析任务是不是应该自动执行
    首先相关任务应该包含在整个项目sync的执行路径上
    还有就是根据token的版本号决定是否需要触发下载新版本token以及解析

    关于字体，字体是不是也应该和token一起提供，然后我们下载并把字体放到res/font目录下。

    解析token整体步骤
    1. 把所有的token解析到map中保存
    2. 进行处理和检查 比如对于字体检查res/font下有没有对应字体
    3. 把map中的键值对，变成xml文件。
*/

class TokenParseGradlePlugin : Plugin<Project> {

    // token文件放的位置。如果每次根据版本号确定是否下载，解析完之后应该可以删除掉。 版本号文件也可以放这里 比如token_version.txt
    var tokenRelatedFilePath = ""
    var resValuesToInjectPath = ""
    var resFontsToInjectPath = ""

    override fun apply(target: Project) {
        with(target) {

            tokenRelatedFilePath = project.file(listOf("buildSrc", "src", "main", "kotlin", "files").joinToString(File.separator)).absolutePath + File.separator
            resValuesToInjectPath = project.file(listOf("app", "src", "main", "res", "values").joinToString(File.separator)).absolutePath + File.separator
            resFontsToInjectPath = project.file(listOf("app", "src", "main", "res", "font").joinToString(File.separator)).absolutePath + File.separator

            tasks.register("createEmptyFile") {

                group = "common_ui"
                doLast {
                    print(123)
                    val valuesDir = file("app/src/main/res/values")
                    valuesDir.mkdirs()

                    val emptyFile = File(valuesDir, "empty_file.xml")
                    emptyFile.createNewFile()
                }
            }

            tasks.register("parseToken") {
                group = "common_ui"
                description = "download token.json and then parse it to xml"

                doLast {
                    TokenUtil.apply {
                        colorsXMLFileAbsPath = resValuesToInjectPath + "common_color.xml"
                        dimensXMLFileAbsPath = resValuesToInjectPath + "common_dimen.xml"
                        aliasStyleXMLFileAbsPath= resValuesToInjectPath + "common_alias_style.xml"
                        parseToken(tokenRelatedFilePath + "tokens-light.json", TokenUtil.TokenType.LIGHT)
                    }
                    // 只有上面解析了token才能知道需要哪些字体，才能检查这些字体存不存在
                    checkFontExist()
                    //TokenUtil.writeToXml()
                }
            }
        }
    }

    // 检查网络上的token版本号和当前的版本号是否相同,以此决定是否需要更新
    private fun isNeedToUpdateToken() : Boolean {
        return false
    }

    // 下载toekn的json文件，以及字体相关文件
    private fun downloadNewTokenRelatedFiles() {

    }

    private fun checkFontExist() {
        print("检测token中需要的字体是否都存在")
        val fonts = File(resFontsToInjectPath).listFiles()?.map{
            it.name.substringBeforeLast(".")
        }

        if (fonts != null) {
            val vaild = TokenUtil.getAllFonts().all{ fontName ->
                fonts.contains(fontName)
            }
            if (!vaild)
                print("有字体不存在 结束token相关任务")
        } else {
            println("$resFontsToInjectPath 不存在")
        }
    }

    private fun downloadFont() {

    }
}

apply<TokenParseGradlePlugin>()





