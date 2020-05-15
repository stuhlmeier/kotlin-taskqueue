# kotlin-taskqueue (WIP)

This library provides an asynchronous task queue interface for Kotlin.

## Types

### `Executable`/`Task`

(TODO)

### `TaskExecutor`

`TaskExecutor` provides an interface through which `Executable`s may be submitted for execution in a cancellable way.

Conceptually, this is equivalent to Java's `Executor`s/`ExecutorService`s, except that it uses coroutines instead of futures.

Implementations of `TaskExecutor.execute` must be cancellable or must clearly indicate otherwise.<br>
Implementations of `TaskExecutor.close` should cancel all running and pending tasks and return immediately.

#### `CoroutineExecutor`

This implementation executes tasks by starting new coroutines in a specified coroutine scope.
As Kotlin itself does the heavy lifting, this should be good enough for most requirements,
and is functionally equivalent to immediately starting a coroutine with the specified context and awaiting its completion.

On the JVM, this class also implements `Executor` and `ExecutorService` (TODO).

#### `CoroutinePoolExecutor`

This implementation executes tasks on a specified number of worker coroutines.
Its coroutine context is configurable, so it can make use of any dispatcher.
Tasks are distributed via `Channel`'s fan-out mechanism.

**Note that this is a coroutine pool, not a thread pool**, and as such, *it has limited concurrency* (equal to the number of workers.)
This implementation limits the number of running tasks at all times, regardless of whether execution is suspended.
If all workers are waiting for suspended tasks to complete,
no pending tasks will be started until one of the running tasks completes.
A task that suspends forever, for example, will permanently prevent a worker from accepting new tasks.

If you don't need to limit concurrency, or you're not sure what executor implementation to use,
you probably want `CoroutineExecutor` instead.

On the JVM, this class also implements `Executor` and `ExecutorService` (TODO).

#### Factory functions

- `executor([context])`
- `poolExecutor(workers, [context])`
- `sequentialExecutor([context])`

### `TaskQueue`

(TODO)
