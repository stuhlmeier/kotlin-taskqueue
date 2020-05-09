package me.stuhlmeier.taskqueue

import kotlinx.coroutines.CoroutineScope

expect fun runTest(block: suspend CoroutineScope.() -> Unit)
