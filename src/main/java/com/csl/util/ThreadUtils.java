package com.csl.util;

import org.slf4j.MDC;

import java.util.concurrent.*;

import static com.csl.web.jcmdoversocket.CSLWebSocketForJcmd.X_CORRELATION_ID;

/**
 * Utils for threads
 */
public class ThreadUtils {

    /**
     * Executor for thread running at fixed rate and with timeout that transfers the X_Correlation_ID from parent thread to child thread.
     * @param command The task to execute.
     * @param initialDelay The delay before executing the task.
     * @param period The period between executions.
     * @param unit The time unit of the delay.
     * @param timeout The timeout of the task, in milliseconds.
     * @param timeoutUnit The time unit of the timeout.
     * @return the given concurrent executor
     */
    public static ScheduledExecutorService correlatedSingleThreadScheduledAtFixedRate(Runnable command, long initialDelay,
                                                                                      long period,
                                                                                      TimeUnit unit,
                                                                                      long timeout,
                                                                                      TimeUnit timeoutUnit) {
        String xCorrelationId = MDC.get(X_CORRELATION_ID);
        ScheduledExecutorService threadExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduleAtFixedRatedWithTimeout(threadExecutor, ()->{
            MDC.put(X_CORRELATION_ID, xCorrelationId);
            command.run();
        }, initialDelay, period, unit, timeout, timeoutUnit);

        return threadExecutor;
    }
    /**
     * Schedule a task to be executed after a delay.
     *
     * @param scheduledExecutorService The executor service to use.
     * @param command                  The task to execute.
     * @param initialDelay             The delay before executing the task.
     * @param period                   The period between executions.
     * @param unit                     The time unit of the delay.
     * @param timeout                  The timeout of the task, in milliseconds.
     */
    public static void scheduleAtFixedRatedWithTimeout(ScheduledExecutorService scheduledExecutorService, Runnable command, long initialDelay, long period, TimeUnit unit, long timeout, TimeUnit timeoutUnit) {
        ScheduledExecutorService commandExecutor = Executors.newSingleThreadScheduledExecutor();
        Runnable task = () -> {
            Future<?> future = commandExecutor.submit(command);
            try {
                future.get(timeout, timeoutUnit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | TimeoutException e) {
                e.printStackTrace(System.err);
            } finally {
                future.cancel(true);
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(task, initialDelay, period, unit);
    }
}
