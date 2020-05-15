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

class ExponentialBackoffScheduler(
    val coefficient: Long,
    val base: Int,
    val maxDelay: Long = Long.MAX_VALUE
) : TaskScheduler.Factory {
    init {
        require(coefficient > 0) { "${::coefficient.name} must be positive" }
        require(base > 0) { "${::base.name} must be positive" }
        require(maxDelay > 0) { "${::maxDelay.name} must be positive" }
    }

    override fun create(task: Task<*>): TaskScheduler {
        return object : TaskScheduler {
            private var delay: Long = coefficient

            override fun taskSucceeded(task: Task<*>, timestamp: Timestamp) = Unit
            override fun taskFailed(task: Task<*>, timestamp: Timestamp): RescheduleAction? {
                return if (delay < maxDelay) {
                    // Prevent overflow by clamping to maxDelay
                    val maxNoOverflow: Long = maxDelay / base
                    delay = when {
                        delay >= maxNoOverflow -> maxDelay
                        else -> delay * base
                    }
                    RescheduleAction.RescheduleAt(timestamp + delay)
                } else RescheduleAction.RescheduleAt(timestamp + maxDelay)
            }
        }
    }
}
