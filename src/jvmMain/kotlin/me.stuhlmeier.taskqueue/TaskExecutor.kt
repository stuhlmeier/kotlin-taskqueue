package me.stuhlmeier.taskqueue

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

actual abstract class AbstractExecutor : TaskExecutor, Executor {
    override fun execute(command: Runnable) {
        GlobalScope.launch { execute { command.run() } }
    }
}
