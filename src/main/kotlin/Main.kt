import com.charleskorn.kaml.Yaml
import kotlinx.serialization.*
import java.io.File
import java.io.FileNotFoundException
import kotlin.system.exitProcess

@Serializable
data class Target(val dependencies: List<String>, val target: String, val run: String) {
    fun satisfy(allTargets: Map<String, Target>): Boolean {
        var updateRequired = false
        val targetFile = File(target)
        for (key in dependencies) {
            updateRequired = allTargets[key]?.satisfy(allTargets) ?: satisfyFile(targetFile, key) || updateRequired
        }
        if (updateRequired) {
            println(run)
        }
        return updateRequired
    }

    fun satisfyFile(targetFile: File, name: String): Boolean {
        val f = File(name)
        if (!f.exists()) {
            throw FileNotFoundException("Required file not found: $name")
        }
        return !targetFile.exists() || targetFile.lastModified() < f.lastModified()
    }
}

fun main(/*args: Array<String>*/) {
    val input = """
        compile:
          dependencies:
          - main.c
          target: main.o
          run: gcc -c main.c -o main.o
        build:
          dependencies:
          - compile
          target: main
          run: gcc main.o -o main
    """.trimIndent()

    val allTargets: Map<String, Target> = Yaml.default.decodeFromString(input)
    try {
        println(allTargets["build"]?.satisfy(allTargets))
    } catch (e: FileNotFoundException) {
        System.err.println(e.message)
        exitProcess(1)
    }
}
