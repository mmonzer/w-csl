package com.csl.util;

import com.csl.logger.LoggerInterfaces;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;

import java.util.concurrent.*;

import static com.csl.logger.LoggerConstants.*;

/**
 * Utils for correlation of threads
 */
public final class ThreadUtils {

    /**
     * Executor for thread running at fixed rate that transfers the X_Correlation_ID from parent thread to child thread.
     * @param command The task to execute.
     * @param initialDelay The delay before executing the task.
     * @param period The period between executions.
     * @param timeUnit The time unit of the timeout.
     * @return the given concurrent executor
     */
    public static ScheduledExecutorService correlatedSingleThreadScheduledAtFixedRate(Runnable command, long initialDelay,
                                                                                      long period,
                                                                                      TimeUnit timeUnit) {
        ScheduledExecutorService threadExecutor = Executors.newSingleThreadScheduledExecutor();

        singleThreadScheduledAtFixedRate(threadExecutor, command, initialDelay, period, timeUnit, MDC.get(X_CORRELATION_ID), MDC.get(ENDPOINT));

        return threadExecutor;
    }

    /**
     * Executor for thread running at fixed rate that transfers the X_Correlation_ID from parent thread to child thread.
     * @param threadExecutor thread executor.
     * @param command The task to execute.
     * @param initialDelay The delay before executing the task.
     * @param period The period between executions.
     * @param timeUnit The time unit of the timeout.
     */
    public static void correlatedSingleThreadScheduledAtFixedRate(
            ScheduledExecutorService threadExecutor,
            Runnable command,
            long initialDelay,
            long period,
            TimeUnit timeUnit) {
        singleThreadScheduledAtFixedRate(threadExecutor, command, initialDelay, period, timeUnit, MDC.get(X_CORRELATION_ID), MDC.get(ENDPOINT));
    }

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
        String endpoint = MDC.get(ENDPOINT);
        ScheduledExecutorService threadExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduleAtFixedRatedWithTimeout(threadExecutor, ()->{
            MDC.put(X_CORRELATION_ID, xCorrelationId);
            MDC.put(ENDPOINT, endpoint);
            command.run();
        }, initialDelay, period, unit, timeout, timeoutUnit);

        return threadExecutor;
    }

    /**
     * Executor for thread running at fixed rate that creates the X_Correlation_ID .
     * @param threadExecutor thread executor.
     * @param command The task to execute.
     * @param initialDelay The delay before executing the task.
     * @param period The period between executions.
     * @param timeUnit The time unit of the timeout.
     */
    public static void uncorrelatedSingleThreadScheduledAtFixedRate(
            ScheduledExecutorService threadExecutor,
            Runnable command,
            long initialDelay,
            long period,
            TimeUnit timeUnit) {
        uncorrelatedSingleThreadScheduledAtFixedRate(threadExecutor, command, initialDelay, period, timeUnit, MDC.get(ENDPOINT));
    }

    /**
     * Executor for thread running at fixed rate that creates the X_Correlation_ID .
     * @param threadExecutor thread executor.
     * @param command The task to execute.
     * @param initialDelay The delay before executing the task.
     * @param period The period between executions.
     * @param timeUnit The time unit of the timeout.
     */
    public static void uncorrelatedSingleThreadScheduledAtFixedRate(
            ScheduledExecutorService threadExecutor,
            Runnable command,
            long initialDelay,
            long period,
            TimeUnit timeUnit, String endpoint) {
        singleThreadScheduledAtFixedRate(threadExecutor, command, initialDelay, period, timeUnit, CorrelationUtils.createXCorrelationId(), endpoint);
    }

    /**
     * Executor for thread running at fixed rate that creates the X_Correlation_ID .
     * @param threadExecutor thread executor.
     * @param command The task to execute.
     * @param initialDelay The delay before executing the task.
     * @param period The period between executions.
     * @param timeUnit The time unit of the timeout.
     */
    public static void uncorrelatedSingleThreadScheduledAtFixedRate(
            ScheduledExecutorService threadExecutor,
            Runnable command,
            long initialDelay,
            long period,
            TimeUnit timeUnit, String endpoint, String initializerService) {
        singleThreadScheduledAtFixedRate(threadExecutor, command, initialDelay, period, timeUnit, CorrelationUtils.createXCorrelationId(), endpoint, initializerService);
    }

    /**
     * Executor for thread running at fixed rate that creates the X_Correlation_ID .
     * @param threadExecutor thread executor.
     * @param command The task to execute.
     * @param initialDelay The delay before executing the task.
     * @param period The period between executions.
     * @param timeUnit The time unit of the timeout.
     */
    public static void uncorrelatedSingleThreadScheduledAtFixedRate(
            ScheduledExecutorService threadExecutor,
            Runnable command,
            long initialDelay,
            long period,
            TimeUnit timeUnit, String endpoint, LoggerInterfaces initializerService) {
        singleThreadScheduledAtFixedRate(threadExecutor, command, initialDelay, period, timeUnit, CorrelationUtils.createXCorrelationId(), endpoint, initializerService.toString());
    }

    /**
     * Launches a task in a single thread as scheduleAtFixedRate but adds the X-Correlation-ID and the endpoint to the MDC environment of the new thread.
     * @param threadExecutor executor for thread
     * @param command callback to execute
     * @param initialDelay init delay to start the thread
     * @param period frequency to execute the thread
     * @param timeUnit time unit for initialDelay and period
     * @param xCorrelationId X-Correlation-ID to add to MDC env of the new thread
     * @param endpoint endpoint to add to MDC env of the new thread
     * @return the future of the execution
     */
    private static @NotNull ScheduledFuture<?> singleThreadScheduledAtFixedRate(ScheduledExecutorService threadExecutor, Runnable command, long initialDelay, long period, TimeUnit timeUnit, String xCorrelationId, String endpoint) {
        return singleThreadScheduledAtFixedRate(threadExecutor, command, initialDelay, period, timeUnit, xCorrelationId, endpoint, LoggerInterfaces.CSL_CLIENT.toString());
    }

    /**
     * Launches a task in a single thread as scheduleAtFixedRate but adds the X-Correlation-ID the endpoint and the initializer service to the MDC environment of the new thread.
     * @param threadExecutor executor for thread
     * @param command callback to execute
     * @param initialDelay init delay to start the thread
     * @param period frequency to execute the thread
     * @param timeUnit time unit for initialDelay and period
     * @param xCorrelationId X-Correlation-ID to add to MDC env of the new thread
     * @param endpoint endpoint to add to MDC env of the new thread
     * @param initializerService initializerService to add to MDC env of the new thread
     * @return the future of the execution
     */
    private static @NotNull ScheduledFuture<?> singleThreadScheduledAtFixedRate(ScheduledExecutorService threadExecutor, Runnable command, long initialDelay, long period, TimeUnit timeUnit, String xCorrelationId, String endpoint, String initializerService) {
        return threadExecutor.scheduleAtFixedRate(
                () -> {
                    MDC.put(X_CORRELATION_ID, CorrelationUtils.getFormattedXCorrelationId(xCorrelationId, initializerService));
                    MDC.put(ENDPOINT, endpoint);
                    MDC.put(LOG_TYPE, APPLICATIVE);
                    command.run();
                }, initialDelay, period, timeUnit);
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
                // e.printStackTrace();
                Thread.currentThread().interrupt();
            } finally {
                future.cancel(true);
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

}
