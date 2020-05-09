# kotlin-taskqueue (WIP)

This library provides an asynchronous task queue implementation for Kotlin.

## Types

### `Task<R>` = `suspend () -> R`

### `TaskExecutor`

`TaskExecutor` provides an interface through which `Task`s may be submitted for execution in a cancellable way.

Conceptually, this is equivalent to Java's `Executor`s/`ExecutorService`s, except that it uses coroutines and suspending functions instead of futures.

Implementations of `TaskExecutor.execute` must be cancellable or must clearly indicate otherwise.<br>
Implementations of `TaskExecutor.close` should cancel all running and pending tasks and return immediately.

#### `PoolExecutor`

`PoolExecutor` is a `TaskExecutor` implementation that executes tasks on a number of worker coroutines.
Its coroutine context is configurable, so it can make use of any dispatcher.
Tasks are distributed via `Channel`'s fan-out mechanism.

On the JVM, this class also implements `Executor` and `ExecutorService` (TODO).

#### Factory functions

- `poolExecutor(workers, [context])`
- `sequentialExecutor([context])`

### `TaskQueue`

(TODO)
