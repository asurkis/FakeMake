package ru.itmo.asurkis.test.fakemake.tests

import org.junit.jupiter.api.assertThrows
import ru.itmo.asurkis.test.fakemake.Target
import ru.itmo.asurkis.test.fakemake.TargetLoopException
import java.io.FileNotFoundException

import kotlin.test.Test
import kotlin.test.assertEquals

class DoubleRunException(val name: String) : Exception() {
    override val message: String
        get() = "Target $name had been run twice"
}

class SingleRunTarget(
    val name: String,
    val allowedRuns: Int,
    dependencies: List<String> = listOf(),
    targetFileName: String? = null,
    runCommand: String? = null
) : Target(dependencies, targetFileName, runCommand) {
    var nRuns = 0

    override fun makeRun() {
        if (++nRuns > allowedRuns) {
            throw DoubleRunException(name)
        }
    }
}

class Tests {
    @Test
    fun singleRun() {
        val one = SingleRunTarget("one", 1)
        val map = mapOf(Pair("one", one))
        one.satisfy(map)
        assertEquals(1, one.nRuns)
    }

    @Test
    fun reuseRuns() {
        val one = SingleRunTarget("one", 1)
        val map = mapOf(Pair("one", one))
        one.satisfy(map)
        one.satisfy(map)
        assertEquals(1, one.nRuns)
    }

    @Test
    fun satisfyDependencies() {
        val one = SingleRunTarget("one", 1)
        val two = SingleRunTarget("two", 2, listOf("one"))
        val map = mapOf(Pair("one", one), Pair("two", two))
        two.satisfy(map)
        assertEquals(1, one.nRuns)
        assertEquals(1, two.nRuns)
    }

    @Test
    fun reuseDependencies() {
        val one = SingleRunTarget("one", 1)
        val two = SingleRunTarget("two", 1, listOf("one"))
        val map = mapOf(Pair("one", one), Pair("two", two))
        one.satisfy(map)
        two.satisfy(map)
        assertEquals(1, one.nRuns)
        assertEquals(1, two.nRuns)
    }

    @Test
    fun reuseDependencies2() {
        val one = SingleRunTarget("one", 1, listOf("three"))
        val two = SingleRunTarget("two", 1, listOf("three"))
        val three = SingleRunTarget("three", 1)
        val map = mapOf(Pair("one", one), Pair("two", two), Pair("three", three))
        one.satisfy(map)
        two.satisfy(map)
        assertEquals(1, one.nRuns)
        assertEquals(1, two.nRuns)
        assertEquals(1, three.nRuns)
    }

    @Test
    fun leaveIndependent() {
        val one = SingleRunTarget("one", 1)
        val two = SingleRunTarget("two", 0, listOf("one"))
        val map = mapOf(Pair("one", one), Pair("two", two))
        one.satisfy(map)
        assertEquals(1, one.nRuns)
    }

    @Test
    fun leaveIndependent2() {
        val one = SingleRunTarget("one", 1)
        val two = SingleRunTarget("two", 1, listOf("one"))
        val three = SingleRunTarget("three", 0, listOf("one"))
        val map = mapOf(Pair("one", one), Pair("two", two), Pair("three", three))
        two.satisfy(map)
        assertEquals(1, one.nRuns)
        assertEquals(1, two.nRuns)
    }

    @Test
    fun throwNotFound() {
        /* File that would not be present */
        val one = SingleRunTarget("one", 0, listOf("qwerasdfzxcv"))
        val map = mapOf(Pair("one", one))
        assertThrows<FileNotFoundException> { one.satisfy(map) }
        assertEquals(0, one.nRuns)
    }

    @Test
    fun throwLoop() {
        val one = SingleRunTarget("one", 0, listOf("two"))
        val two = SingleRunTarget("two", 0, listOf("one"))
        val map = mapOf(Pair("one", one), Pair("two", two))
        assertThrows<TargetLoopException> { one.satisfy(map) }
    }

    @Test
    fun loopExceptionMessage() {
        val one = SingleRunTarget("one", 0, listOf("two"))
        val two = SingleRunTarget("two", 0, listOf("one"))
        val three = SingleRunTarget("three", 0, listOf("one"))
        val map = mapOf(Pair("one", one), Pair("two", two), Pair("three", three))
        assertThrows<TargetLoopException>("Loop of targets detected: one -> two -> one") {
            three.satisfy(map)
        }
    }
}
