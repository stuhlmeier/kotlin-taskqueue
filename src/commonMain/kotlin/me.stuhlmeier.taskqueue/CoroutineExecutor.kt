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
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class CoroutineExecutor(context: CoroutineContext = EmptyCoroutineContext) : AbstractExecutor() {
    private val job = Job(context[Job])
    private val scope = CoroutineScope(context + job)

    override suspend fun <Result> execute(executable: Executable<Result>): Result {
        val result = scope.async(start = CoroutineStart.LAZY) { executable() }
        try {
            return result.await()
        } catch (e: CancellationException) {
            result.cancel(e)
            throw e
        }
    }

    override fun close() {
        scope.cancel(CancellationException("${CoroutineExecutor::class.simpleName} was cancelled"))
    }
}
