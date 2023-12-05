package plugins.plugins


/**
 * Precompiled [token.parse.gradle.kts][plugins.plugins.Token_parse_gradle] script plugin.
 *
 * @see plugins.plugins.Token_parse_gradle
 */
class Token_parsePlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("plugins.plugins.Token_parse_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
