package me.stuhlmeier.taskqueue

import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// TODO: these tests may be suboptimal because they rely somewhat on proper timing.
// Since they all use sequential executors (1 worker) this shouldn't result in any odd bugs,
// but this will probably be replaced with a sort of "checkpoint" system, similar to how
// tests are handled in kotlinx.coroutines (cf. TestBase)
class CoroutinePoolExecutorTests {
    @Test
    fun testQueuedTaskCompletes() = runTest {
        val executor = poolExecutor(1)

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
        val executor = poolExecutor(1)

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
    fun testCloseExecutorCancelsPendingTask() = runTest {
        val executor = sequentialExecutor()

        var endFlag1 = false
        var endFlag2 = false

        lateinit var job: Deferred<*>

        launch {
            supervisorScope {
                launch {
                    executor.execute(object : Executable<Unit> {
                        override suspend fun invoke() {
                            delay(1000)
                            endFlag1 = true
                        }
                    })
                }
                job = async {
                    executor.execute(object : Executable<Unit> {
                        override suspend fun invoke() {
                            endFlag2 = true
                        }
                    })
                }
            }
        }

        launch {
            delay(500)
            executor.close()
        }

        delay(1500)

        withTimeout(JOIN_TIMEOUT) {
            assertFailsWith<CancellationException> { job.await() }
        }

        assertFalse(endFlag1)
        assertFalse(endFlag2)
    }

    @Test
    fun testClosedExecutorThrows() = runTest {
        val executor = poolExecutor(1)

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
        val executor = poolExecutor(1)

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

    @Test
    fun testCancelExecuteCancelsPendingTask() = runTest {
        val executor = sequentialExecutor()

        var endFlag = false

        lateinit var job: Deferred<*>

        launch {
            supervisorScope {
                launch {
                    executor.execute(object : Executable<Unit> {
                        override suspend fun invoke() {
                            delay(1000)
                        }
                    })
                }
                job = async {
                    executor.execute(object : Executable<Unit> {
                        override suspend fun invoke() {
                            endFlag = true
                        }
                    })
                }
            }
        }

        launch {
            delay(500)
            job.cancel()
        }

        delay(1500)

        withTimeout(JOIN_TIMEOUT) { job.join() }

        assertFalse(endFlag)
    }
}
