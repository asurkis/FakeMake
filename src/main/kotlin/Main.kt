import com.charleskorn.kaml.Yaml
import kotlinx.serialization.*
import java.io.File
import java.io.FileNotFoundException
import kotlin.system.exitProcess

class TargetLoopException(
    val loopStart: Target,
    var loopComplete: Boolean = false,
    val loopParts: MutableList<String> = mutableListOf()
) : Exception() {
    override val message: String
        get() = "Loop of targets detected: ${loopParts.joinToString(" -> ")}"
}

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

    fun satisfy(targetMap: Map<String, Target>): Boolean {
        if (isSatisfied) return true
        if (dfsProcess) {
            throw TargetLoopException(this)
        }
        dfsProcess = true
        var runRequired = dependencies.isEmpty()
        val targetFile = if (targetFileName != null) File(targetFileName) else null
        for (key in dependencies) {
            try {
                if (targetMap[key]?.satisfy(targetMap) ?: satisfyFile(targetFile, key)) {
                    runRequired = true
                }
            } catch (e: TargetLoopException) {
                if (!e.loopComplete) {
                    e.loopParts.add(0, key)
                    if (this === e.loopStart) {
                        e.loopParts.add(key)
                        e.loopComplete = true
                    }
                }
                throw e
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
        if (runCommand != null) {
            println(runCommand)
        }
    }
}

fun main(/*args: Array<String>*/) {
    val input = """
        default:
          dependencies:
          - build
          - compile
        compile:
          dependencies:
          # - main.c
          - build
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
        allTargets["default"]?.satisfy(allTargets)
    } catch (e: Exception) {
        System.err.println(e.message)
        exitProcess(1)
    }
}
