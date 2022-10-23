import com.charleskorn.kaml.Yaml
import kotlinx.serialization.*

@Serializable
data class CommandRecord(val dependencies: Array<String>, val target: String, val run: String)

fun main() {
    val input = """
        compile:
          dependencies:
          - "main.c"
          target: "main.o"
          run: "gcc -c main.c -o main.o"
        build:
          dependencies:
          - "compile"
          target: "main"
          run: "gcc main.o -o main"
    """.trimIndent()
    println(input)
    val targets: Map<String, CommandRecord> = Yaml.default.decodeFromString(input)
    println(Yaml.default.encodeToString(targets))
}
