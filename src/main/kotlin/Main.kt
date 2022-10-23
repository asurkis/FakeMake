import com.charleskorn.kaml.Yaml
import kotlinx.serialization.*
import java.io.File
import java.io.FileNotFoundException
import kotlin.system.exitProcess

@Serializable
data class Target(
    val dependencies: List<String> = listOf(),
    @SerialName("target")
    val targetFileName: String? = null,
    @SerialName("run")
    val runCommand: String? = null
) {
    private var isSatisfied = false
    private var dfsProcess = false

    fun satisfy(allTargets: Map<String, Target>): Boolean {
        if (isSatisfied) return true
        dfsProcess = true
        var runRequired = dependencies.isEmpty()
        val targetFile = if (targetFileName != null) File(targetFileName) else null
        for (key in dependencies) {
            if (allTargets[key]?.satisfy(allTargets) ?: satisfyFile(targetFile, key)) {
                runRequired = true
            }
        }
        if (runRequired) {
            makeRun()
        }
        isSatisfied = true
        dfsProcess = false
        return runRequired
    }

    fun satisfyFile(targetFile: File?, name: String): Boolean {
        val f = File(name)
        if (!f.exists()) {
            throw FileNotFoundException("Required file not found: $name")
        }
        return targetFile == null || !targetFile.exists() || targetFile.lastModified() < f.lastModified()
    }

    fun makeRun() {
        println(runCommand)
    }
}

fun main(/*args: Array<String>*/) {
    val input = """
        default:
          dependencies:
          - build
          - compile
        compile:
          # dependencies:
          # - main.c
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
        println(allTargets["default"]?.satisfy(allTargets))
    } catch (e: FileNotFoundException) {
        System.err.println(e.message)
        exitProcess(1)
    }
}
