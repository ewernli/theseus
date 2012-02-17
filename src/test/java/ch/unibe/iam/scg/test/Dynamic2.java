package ch.unibe.iam.scg.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Dynamic2 extends AbstractExecutorService 
{
	Dynamic2( int a, int b, int c, TimeUnit u, BlockingQueue q )
	{
		//super( a,b,c,u,q);
	}

	 /**
     * Only used to force toArray() to produce a Runnable[].
     */
    private static final Runnable[] EMPTY_RUNNABLE_ARRAY = new Runnable[0];

    /**
     * Permission for checking shutdown
     */
    public static final RuntimePermission shutdownPerm =
        new RuntimePermission("modifyThread");

    /**
     * Queue used for holding tasks and handing off to worker threads.
     */
    private  BlockingQueue<Runnable> workQueue;

    /**
     * Lock held on updates to poolSize, corePoolSize, maximumPoolSize, and
     * workers set.
     */
    private  ReentrantLock mainLock = new ReentrantLock();

    /**
     * Wait condition to support awaitTermination
     */
    private  Condition termination = mainLock.newCondition();


    /**
     * Timeout in nanoseconds for idle threads waiting for work.
     * Threads use this timeout only when there are more than
     * corePoolSize present. Otherwise they wait forever for new work.
     */
    private volatile long  keepAliveTime;

    /**
     * Core pool size, updated only while holding mainLock,
     * but volatile to allow concurrent readability even
     * during updates.
     */
    private volatile int   corePoolSize;

    /**
     * Maximum pool size, updated only while holding mainLock
     * but volatile to allow concurrent readability even
     * during updates.
     */
    private volatile int   maximumPoolSize;

    /**
     * Current pool size, updated only while holding mainLock
     * but volatile to allow concurrent readability even
     * during updates.
     */
    private volatile int   poolSize;

    /**
     * Lifecycle state
     */
    volatile int runState;

    // Special values for runState
    /** Normal, not-shutdown mode */
    static final int RUNNING    = 0;
    /** Controlled shutdown mode */
    static final int SHUTDOWN   = 1;
    /** Immediate shutdown mode */
    static final int STOP       = 2;
    /** Final state */
    static final int TERMINATED = 3;

    /**
     * Handler called when saturated or shutdown in execute.
     */
    private volatile RejectedExecutionHandler handler;

    /**
     * Factory for new threads.
     */
    private volatile ThreadFactory threadFactory;

    /**
     * Tracks largest attained pool size.
     */
    private int largestPoolSize;

    /**
     * Counter for completed tasks. Updated only on termination of
     * worker threads.
     */
    private long completedTaskCount;
    
    /**
     * The default rejected execution handler
     */
    private static final RejectedExecutionHandler defaultHandler =
        null;

    /**
     * Invoke the rejected execution handler for the given command.
     */
    void reject(Runnable command) {
      //  handler.rejectedExecution(command, this);
    }

    /**
     * Create and return a new thread running firstTask as its first
     * task. Call only while holding mainLock
     * @param firstTask the task the new thread should run first (or
     * null if none)
     * @return the new thread, or null if threadFactory fails to create thread
     */
    private Thread addThread(Runnable firstTask) {
       return null;
    }

    /**
     * Create and start a new thread running firstTask as its first
     * task, only if fewer than corePoolSize threads are running.
     * @param firstTask the task the new thread should run first (or
     * null if none)
     * @return true if successful.
     */
    private boolean addIfUnderCorePoolSize(Runnable firstTask) {
        Thread t = null;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (poolSize < corePoolSize)
                t = addThread(firstTask);
        } finally {
            mainLock.unlock();
        }
        if (t == null)
            return false;
        t.start();
        return true;
    }

    /**
     * Create and start a new thread only if fewer than maximumPoolSize
     * threads are running.  The new thread runs as its first task the
     * next task in queue, or if there is none, the given task.
     * @param firstTask the task the new thread should run first (or
     * null if none)
     * @return null on failure, else the first task to be run by new thread.
     */
    private Runnable addIfUnderMaximumPoolSize(Runnable firstTask) {
        Thread t = null;
        Runnable next = null;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (poolSize < maximumPoolSize) {
                next = workQueue.poll();
                if (next == null)
                    next = firstTask;
                t = addThread(next);
            }
        } finally {
            mainLock.unlock();
        }
        if (t == null)
            return null;
        t.start();
        return next;
    }


    /**
     * Get the next task for a worker thread to run.
     * @return the task
     * @throws InterruptedException if interrupted while waiting for task
     */
    Runnable getTask() throws InterruptedException {
        for (;;) {
            switch(runState) {
            case RUNNING: {
                if (poolSize <= corePoolSize)   // untimed wait if core
                    return workQueue.take();
                
                long timeout = keepAliveTime;
                if (timeout <= 0) // die immediately for 0 timeout
                    return null;
                Runnable r =  workQueue.poll(timeout, TimeUnit.NANOSECONDS);
                if (r != null)
                    return r;
                if (poolSize > corePoolSize) // timed out
                    return null;
                // else, after timeout, pool shrank so shouldn't die, so retry
                break;
            }

            case SHUTDOWN: {
                // Help drain queue 
                Runnable r = workQueue.poll();
                if (r != null)
                    return r;
                    
                // Check if can terminate
                if (workQueue.isEmpty()) {
                    interruptIdleWorkers();
                    return null;
                }

                // There could still be delayed tasks in queue.
                // Wait for one, re-checking state upon interruption
                try {
                    return workQueue.take();
                } catch(InterruptedException ignore) {}
                break;
            }

            case STOP:
                return null;
            default:
                assert false; 
            }
        }
    }

    /**
     * Wake up all threads that might be waiting for tasks.
     */
    void interruptIdleWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
        } finally {
            mainLock.unlock();
        }
    }

//    /**
//     * Perform bookkeeping for a terminated worker thread.
//     * @param w the worker
//     */
//    void workerDone(Worker w) {
//        final ReentrantLock mainLock = this.mainLock;
//        mainLock.lock();
//        try {
//            
//        
//            if (--poolSize > 0)
//                return;
//
//            // Else, this is the last thread. Deal with potential shutdown.
//
//            int state = runState;
//            assert state != TERMINATED;
//
//            if (state != STOP) {
//                // If there are queued tasks but no threads, create
//                // replacement thread. We must create it initially
//                // idle to avoid orphaned tasks in case addThread
//                // fails.  This also handles case of delayed tasks
//                // that will sometime later become runnable.
//                if (!workQueue.isEmpty()) { 
//                    Thread t = addThread(null);
//                    if (t != null)
//                        t.start();
//                    return;
//                }
//
//                // Otherwise, we can exit without replacement
//                if (state == RUNNING)
//                    return;
//            }
//
//            // Either state is STOP, or state is SHUTDOWN and there is
//            // no work to do. So we can terminate.
//            termination.signalAll();
//            runState = TERMINATED;
//            // fall through to call terminate() outside of lock.
//        } finally {
//            mainLock.unlock();
//        }
//
//        assert runState == TERMINATED;
//        terminated(); 
//    }

//    /**
//     *  Worker threads
//     */
//    public class Worker implements Runnable {
//
//
//        Worker() {
//           
//        }
//
//        /**
//         * Main run loop
//         */
//        public void run() {
//           
//        }
//    }

    // Public methods




    /**
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     *
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current <tt>RejectedExecutionHandler</tt>.
     *
     * @param command the task to execute
     * @throws RejectedExecutionException at discretion of
     * <tt>RejectedExecutionHandler</tt>, if task cannot be accepted
     * for execution
     * @throws NullPointerException if command is null
     */
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
        for (;;) {
            if (runState != RUNNING) {
                reject(command);
                return;
            }
            if (poolSize < corePoolSize && addIfUnderCorePoolSize(command))
                return;
            if (workQueue.offer(command))
                return;
            Runnable r = addIfUnderMaximumPoolSize(command);
            if (r == command)
                return;
            if (r == null) {
                reject(command);
                return;
            }
            // else retry
        }
    }

    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be
     * accepted. Invocation has no additional effect if already shut
     * down.
     * @throws SecurityException if a security manager exists and
     * shutting down this ExecutorService may manipulate threads that
     * the caller is not permitted to modify because it does not hold
     * {@link java.lang.RuntimePermission}<tt>("modifyThread")</tt>,
     * or the security manager's <tt>checkAccess</tt>  method denies access.
     */
    public void shutdown() {
        // Fail if caller doesn't have modifyThread permission. We
        // explicitly check permissions directly because we can't trust
        // implementations of SecurityManager to correctly override
        // the "check access" methods such that our documented
        // security policy is implemented.
	SecurityManager security = System.getSecurityManager();
	if (security != null) 
            java.security.AccessController.checkPermission(Dynamic2.shutdownPerm);

        boolean fullyTerminated = false;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
           
        } finally {
            mainLock.unlock();
        }
        if (fullyTerminated)
            terminated();
    }



    public List<Runnable> shutdownNow() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isShutdown() {
        return runState != RUNNING;
    }

    /** 
     * Returns true if this executor is in the process of terminating
     * after <tt>shutdown</tt> or <tt>shutdownNow</tt> but has not
     * completely terminated.  This method may be useful for
     * debugging. A return of <tt>true</tt> reported a sufficient
     * period after shutdown may indicate that submitted tasks have
     * ignored or suppressed interruption, causing this executor not
     * to properly terminate.
     * @return true if terminating but not yet terminated.
     */
    public boolean isTerminating() {
        return runState == STOP;
    }

    public boolean isTerminated() {
        return runState == TERMINATED;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException {
       return false;
    }

    /**
     * Invokes <tt>shutdown</tt> when this executor is no longer
     * referenced.
     */ 
    protected void finalize()  {
        shutdown();
    }

    /**
     * Sets the thread factory used to create new threads.
     *
     * @param threadFactory the new thread factory
     * @throws NullPointerException if threadFactory is null
     * @see #getThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null)
            throw new NullPointerException();
        this.threadFactory = threadFactory;
    }

    /**
     * Returns the thread factory used to create new threads.
     *
     * @return the current thread factory
     * @see #setThreadFactory
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * Sets a new handler for unexecutable tasks.
     *
     * @param handler the new handler
     * @throws NullPointerException if handler is null
     * @see #getRejectedExecutionHandler
     */
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null)
            throw new NullPointerException();
        this.handler = handler;
    }

    /**
     * Returns the current handler for unexecutable tasks.
     *
     * @return the current handler
     * @see #setRejectedExecutionHandler
     */
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }

    /**
     * Returns the task queue used by this executor. Access to the
     * task queue is intended primarily for debugging and monitoring.
     * This queue may be in active use.  Retrieving the task queue
     * does not prevent queued tasks from executing.
     *
     * @return the task queue
     */
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    /**
     * Removes this task from the executor's internal queue if it is
     * present, thus causing it not to be run if it has not already
     * started.
     * 
     * <p> This method may be useful as one part of a cancellation
     * scheme.  It may fail to remove tasks that have been converted
     * into other forms before being placed on the internal queue. For
     * example, a task entered using <tt>submit</tt> might be
     * converted into a form that maintains <tt>Future</tt> status.
     * However, in such cases, method {@link ThreadPoolExecutor#purge}
     * may be used to remove those Futures that have been cancelled.
     * 
     *
     * @param task the task to remove
     * @return true if the task was removed
     */
    public boolean remove(Runnable task) {
        return getQueue().remove(task);
    }


    /**
     * Tries to remove from the work queue all {@link Future}
     * tasks that have been cancelled. This method can be useful as a
     * storage reclamation operation, that has no other impact on
     * functionality. Cancelled tasks are never executed, but may
     * accumulate in work queues until worker threads can actively
     * remove them. Invoking this method instead tries to remove them now.
     * However, this method may fail to remove tasks in
     * the presence of interference by other threads.
     */
    public void purge() {
      
    }

    /**
     * Sets the core number of threads.  This overrides any value set
     * in the constructor.  If the new value is smaller than the
     * current value, excess existing threads will be terminated when
     * they next become idle. If larger, new threads will, if needed,
     * be started to execute any queued tasks.
     *
     * @param corePoolSize the new core size
     * @throws IllegalArgumentException if <tt>corePoolSize</tt>
     * less than zero
     * @see #getCorePoolSize
     */
    public void setCorePoolSize(int corePoolSize) {
        
    }

    /**
     * Returns the core number of threads.
     *
     * @return the core number of threads
     * @see #setCorePoolSize
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * Starts a core thread, causing it to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed. This method will return <tt>false</tt>
     * if all core threads have already been started.
     * @return true if a thread was started
     */ 
    public boolean prestartCoreThread() {
        return addIfUnderCorePoolSize(null);
    }

    /**
     * Starts all core threads, causing them to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed. 
     * @return the number of threads started.
     */ 
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addIfUnderCorePoolSize(null))
            ++n;
        return n;
    }

    /**
     * Sets the maximum allowed number of threads. This overrides any
     * value set in the constructor. If the new value is smaller than
     * the current value, excess existing threads will be
     * terminated when they next become idle.
     *
     * @param maximumPoolSize the new maximum
     * @throws IllegalArgumentException if maximumPoolSize less than zero or
     * the {@link #getCorePoolSize core pool size}
     * @see #getMaximumPoolSize
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
       
    }

    /**
     * Returns the maximum allowed number of threads.
     *
     * @return the maximum allowed number of threads
     * @see #setMaximumPoolSize
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * Sets the time limit for which threads may remain idle before
     * being terminated.  If there are more than the core number of
     * threads currently in the pool, after waiting this amount of
     * time without processing a task, excess threads will be
     * terminated.  This overrides any value set in the constructor.
     * @param time the time to wait.  A time value of zero will cause
     * excess threads to terminate immediately after executing tasks.
     * @param unit  the time unit of the time argument
     * @throws IllegalArgumentException if time less than zero
     * @see #getKeepAliveTime
     */
    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0)
            throw new IllegalArgumentException();
        this.keepAliveTime = unit.toNanos(time);
    }

    /**
     * Returns the thread keep-alive time, which is the amount of time
     * which threads in excess of the core pool size may remain
     * idle before being terminated.
     *
     * @param unit the desired time unit of the result
     * @return the time limit
     * @see #setKeepAliveTime
     */
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    /* Statistics */

    /**
     * Returns the current number of threads in the pool.
     *
     * @return the number of threads
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * Returns the approximate number of threads that are actively
     * executing tasks.
     *
     * @return the number of threads
     */
    public int getActiveCount() {
   return 0;
    }

    /**
     * Returns the largest number of threads that have ever
     * simultaneously been in the pool.
     *
     * @return the number of threads
     */
    public int getLargestPoolSize() {
    return 0;
    }

    /**
     * Returns the approximate total number of tasks that have been
     * scheduled for execution. Because the states of tasks and
     * threads may change dynamically during computation, the returned
     * value is only an approximation, but one that does not ever
     * decrease across successive calls.
     *
     * @return the number of tasks
     */
    public long getTaskCount() {
     return 0;
    }

    /**
     * Returns the approximate total number of tasks that have
     * completed execution. Because the states of tasks and threads
     * may change dynamically during computation, the returned value
     * is only an approximation, but one that does not ever decrease
     * across successive calls.
     *
     * @return the number of tasks
     */
    public long getCompletedTaskCount() {
        return 0;
    }

    /**
     * Method invoked prior to executing the given Runnable in the
     * given thread.  This method is invoked by thread <tt>t</tt> that
     * will execute task <tt>r</tt>, and may be used to re-initialize
     * ThreadLocals, or to perform logging. Note: To properly nest
     * multiple overridings, subclasses should generally invoke
     * <tt>super.beforeExecute</tt> at the end of this method.
     *
     * @param t the thread that will run task r.
     * @param r the task that will be executed.
     */
    protected void beforeExecute(Thread t, Runnable r) { }

    /**
     * Method invoked upon completion of execution of the given
     * Runnable.  This method is invoked by the thread that executed
     * the task. If non-null, the Throwable is the uncaught exception
     * that caused execution to terminate abruptly. Note: To properly
     * nest multiple overridings, subclasses should generally invoke
     * <tt>super.afterExecute</tt> at the beginning of this method.
     *
     * @param r the runnable that has completed.
     * @param t the exception that caused termination, or null if
     * execution completed normally.
     */
    protected void afterExecute(Runnable r, Throwable t) { }

    /**
     * Method invoked when the Executor has terminated.  Default
     * implementation does nothing. Note: To properly nest multiple
     * overridings, subclasses should generally invoke
     * <tt>super.terminated</tt> within this method.
     */
    protected void terminated() { }



 



 
	
}
