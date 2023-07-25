package com.csl.util;

import java.util.concurrent.*;

public class SchedulerUtil {
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
