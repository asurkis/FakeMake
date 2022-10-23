package ru.itmo.asurkis.test.fakemake

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
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

class ChildProcessException(val retCode: Int, val command: String) : Exception() {
    override val message: String
        get() = "Command \"$command\" finished with exit code $retCode"
}

class TargetNotFoundException(val name: String) : Exception() {
    override val message: String
        get() = "Target \"$name\" not found"
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
        if (dfsProcess) throw TargetLoopException(this)
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
        if (runRequired) makeRun()
        isSatisfied = true
        dfsProcess = false
        return runRequired
    }

    fun satisfyFile(targetFile: File?, name: String): Boolean {
        val f = File(name)
        if (!f.exists()) throw FileNotFoundException(name)
        return targetFile == null || !targetFile.exists() || targetFile.lastModified() < f.lastModified()
    }

    fun makeRun() {
        if (runCommand == null) return
        println("Running command: $runCommand")
        val pb = ProcessBuilder("bash", "-c", "--", runCommand)
        pb.inheritIO()
        val retCode = pb.start().waitFor()
        if (0 != retCode) {
            throw ChildProcessException(retCode, runCommand)
        }
    }
}

fun main(args: Array<String>) {
    try {
        val inputFile = File("fake.yaml")
        val inputStream = inputFile.inputStream()
        val allTargets: Map<String, Target> = Yaml.default.decodeFromStream(inputStream)
        for (arg in args) {
            allTargets[arg]?.satisfy(allTargets) ?: throw TargetNotFoundException(arg)
        }
        allTargets["default"]?.satisfy(allTargets)
    } catch (e: FileNotFoundException) {
        System.err.println("File not found: ${e.message}")
        exitProcess(1)
    } catch (e: Exception) {
        System.err.println(e.message)
        exitProcess(1)
    }
}
