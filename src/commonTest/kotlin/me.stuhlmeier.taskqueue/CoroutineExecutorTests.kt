package me.stuhlmeier.taskqueue

import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoroutineExecutorTests {
    @Test
    fun testQueuedTaskCompletes() = runTest {
        val executor = executor()

        var endFlag = false

        val job = async {
            executor.execute(object : Executable<Unit> {
                override suspend fun invoke() {
                    endFlag = true
                }
            })
        }

        withTimeout(JOIN_TIMEOUT) { job.await() }

        assertTrue(endFlag)
    }

    @Test
    fun testCloseExecutorCancelsRunningTask() = runTest {
        val executor = executor()

        var beginFlag = false
        var endFlag = false

        val job = async {
            executor.execute(object : Executable<Unit> {
                override suspend fun invoke() {
                    beginFlag = true
                    delay(1000)
                    endFlag = true
                }
            })
        }

        launch {
            delay(500)
            executor.close()
        }

        delay(1500)

        withTimeout(JOIN_TIMEOUT) {
            assertFailsWith<CancellationException> { job.await() }
        }

        assertTrue(beginFlag)
        assertFalse(endFlag)
    }

    @Test
    fun testClosedExecutorThrows() = runTest {
        val executor = executor()

        executor.close()

        assertFailsWith<IllegalStateException> {
            executor.execute(object : Executable<Unit> {
                override suspend fun invoke() {
                    Unit
                }
            })
        }
    }

    @Test
    fun testCancelExecuteCancelsRunningTask() = runTest {
        val executor = executor()

        var beginFlag = false
        var endFlag = false

        val job = async {
            executor.execute(object : Executable<Unit> {
                override suspend fun invoke() {
                    beginFlag = true
                    delay(1000)
                    endFlag = true
                }
            })
        }

        launch {
            delay(500)
            job.cancel()
        }

        delay(1500)

        withTimeout(JOIN_TIMEOUT) { job.join() }

        assertTrue(beginFlag)
        assertFalse(endFlag)
    }
}
