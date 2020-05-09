/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package me.stuhlmeier.taskqueue

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class PoolExecutor(val workers: Int, context: CoroutineContext = EmptyCoroutineContext) : AbstractExecutor() {
    private data class QueuedTask<Result>(val task: Task<Result>, val job: CompletableDeferred<Result>)

    private val job = Job(context[Job])
    private val scope = CoroutineScope(context + job)
    private val queue = Channel<QueuedTask<*>>(Channel.RENDEZVOUS)

    private fun createCancellationException() = CancellationException("${PoolExecutor::class.simpleName} was cancelled")

    init {
        require(workers > 0) { "${::workers.name} must be positive" }

        scope.launch(CoroutineName("$this")) {
            try {
                coroutineScope {
                    repeat(workers) { n ->
                        launch(CoroutineName("$this/worker$n")) {
                            for (queued in queue) {
                                // Skip if necessary
                                if (!isActive) {
                                    queued.job.cancel(createCancellationException())
                                    continue
                                }
                                if (queued.job.isCompleted) continue

                                @Suppress("UNCHECKED_CAST")
                                queued as QueuedTask<Any?>

                                try {
                                    val result = withContext(queued.job) { queued.task() }
                                    queued.job.complete(result)
                                } catch (e: CancellationException) {
                                    queued.job.cancel(e)
                                } catch (e: Exception) {
                                    queued.job.completeExceptionally(e)
                                }
                            }
                        }
                    }
                }
            } finally {
                queue.cancel()
            }
        }
    }

    override suspend fun <Result> execute(task: Task<Result>): Result {
        if (!scope.isActive) throw IllegalStateException("${PoolExecutor::class.simpleName} is closed")

        val job = CompletableDeferred<Result>(this.job)
        try {
            queue.send(QueuedTask(task, job))
            return job.await()
        } catch (e: CancellationException) {
            job.cancel(e)
            throw e
        }
    }

    override fun close() {
        queue.cancel(createCancellationException())
        scope.cancel(createCancellationException())
    }
}
