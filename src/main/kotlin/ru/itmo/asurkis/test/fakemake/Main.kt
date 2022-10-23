package ru.itmo.asurkis.test.fakemake

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.*
import java.io.File
import java.io.FileNotFoundException
import kotlin.system.exitProcess

class SectionLoopException(
    val loopStart: Section,
    var loopComplete: Boolean = false,
    val loopParts: MutableList<String> = mutableListOf()
) : Exception() {
    override val message: String
        get() = "Loop of sections detected: ${loopParts.joinToString(" -> ")}"
}

class ChildProcessException(val retCode: Int, val command: String) : Exception() {
    override val message: String
        get() = "Command \"$command\" finished with exit code $retCode"
}

class SectionNotFoundException(val name: String) : Exception() {
    override val message: String
        get() = "Section \"$name\" not found"
}

@Serializable
open class Section(
    val dependencies: List<String> = listOf(),
    val target: String? = null,
    @SerialName("run")
    val runCommand: String? = null
) {
    private var isSatisfied = false
    private var dfsProcess = false

    fun satisfy(sectionMap: Map<String, Section>): Boolean {
        if (isSatisfied) return true
        if (dfsProcess) throw SectionLoopException(this)
        dfsProcess = true
        var runRequired = dependencies.isEmpty()
        val targetFile = if (target != null) File(target) else null
        for (key in dependencies) {
            try {
                if (sectionMap[key]?.satisfy(sectionMap) ?: satisfyFile(targetFile, key)) {
                    runRequired = true
                }
            } catch (e: SectionLoopException) {
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

    open fun makeRun() {
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
        val allSections: Map<String, Section> = Yaml.default.decodeFromStream(inputStream)
        if (args.isEmpty()) {
            allSections["default"]?.satisfy(allSections)
        } else {
            for (arg in args) {
                allSections[arg]?.satisfy(allSections) ?: throw SectionNotFoundException(arg)
            }
        }
    } catch (e: FileNotFoundException) {
        System.err.println("File not found: ${e.message}")
        exitProcess(1)
    } catch (e: Exception) {
        System.err.println(e.message)
        exitProcess(1)
    }
}
