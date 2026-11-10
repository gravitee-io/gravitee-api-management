/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.handlers.api.manager.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.definition.approval.ApprovalOutcomeCommand;
import io.gravitee.gamma.definition.approval.ApprovalRequestCommand;
import io.gravitee.gateway.handlers.api.manager.ApprovalCommandWriter;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.CommandTags;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.management.model.MessageRecipient;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.reactivex.rxjava3.core.Completable;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Writes the approval commands through the command repository, which is the bridge client on a hybrid gateway, so the
 * gateway reaches the control plane the way it already does for subscription failures.
 *
 * <p>Writes run on their own fixed-size pool, not {@code Schedulers.io()}: that one is an unbounded cached pool, and a
 * burst of held calls writing at once would otherwise spawn one thread per write. The writes accepted and not yet done
 * are bounded as well: when the repository or the bridge hangs, each held call would otherwise add a write that waits
 * for a thread. A write asked for beyond {@link #MAX_WAITING_WRITES} is refused with a {@link RejectedExecutionException}
 * and its call is not run. The place of a write is freed when the repository returns, or when it is disposed before it
 * started; a write disposed while running keeps its place, since its thread is still taken. Once shut down, the writer
 * refuses every write with a {@link RejectedExecutionException} of its own.
 */
public class ApprovalCommandWriterImpl implements ApprovalCommandWriter, MeterBinder {

    static final int WRITER_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    static final int MAX_WAITING_WRITES = 1_000;

    private static final Duration REQUEST_TIME_TO_LIVE = Duration.ofMinutes(10);
    private static final Duration OUTCOME_TIME_TO_LIVE = Duration.ofHours(1);

    private final CommandRepository commandRepository;
    private final ObjectMapper objectMapper;
    private final Node node;
    private final Clock clock;
    private final ThreadPoolExecutor executor;
    private final Semaphore places;
    private final int capacity;
    private final AtomicLong refused = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    public ApprovalCommandWriterImpl(CommandRepository commandRepository, ObjectMapper objectMapper, Node node, Clock clock) {
        this(commandRepository, objectMapper, node, clock, writerPool(WRITER_POOL_SIZE), MAX_WAITING_WRITES);
    }

    ApprovalCommandWriterImpl(
        CommandRepository commandRepository,
        ObjectMapper objectMapper,
        Node node,
        Clock clock,
        ThreadPoolExecutor executor,
        int capacity
    ) {
        this.commandRepository = commandRepository;
        this.objectMapper = objectMapper;
        this.node = node;
        this.clock = clock;
        this.executor = executor;
        this.capacity = capacity;
        this.places = new Semaphore(capacity);
    }

    static ThreadPoolExecutor writerPool(int threads) {
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "gio.approval-command-writer-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
            threads,
            threads,
            15L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            threadFactory
        );
        pool.allowCoreThreadTimeOut(true);
        return pool;
    }

    @Override
    public Completable send(ApprovalRequestCommand request) {
        return write(CommandTags.APPROVAL_REQUEST, request.getOrganizationId(), request.getEnvironmentId(), request, REQUEST_TIME_TO_LIVE);
    }

    @Override
    public Completable send(ApprovalOutcomeCommand outcome) {
        return write(CommandTags.APPROVAL_OUTCOME, outcome.getOrganizationId(), outcome.getEnvironmentId(), outcome, OUTCOME_TIME_TO_LIVE);
    }

    /** Refuses every new write, and stops the writer threads once the writes already accepted are done. */
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("human_approval_commands_waiting", this, ApprovalCommandWriterImpl::waiting)
            .description("Approval commands accepted and not yet written")
            .register(registry);
        FunctionCounter.builder("human_approval_commands_refused", refused, AtomicLong::get)
            .description("Approval commands refused because too many were waiting")
            .register(registry);
        FunctionCounter.builder("human_approval_commands_failed", failed, AtomicLong::get)
            .description("Approval commands the repository or the bridge failed to write")
            .register(registry);
    }

    // Visible for testing
    int waiting() {
        return capacity - places.availablePermits();
    }

    private Completable write(CommandTags tag, String organizationId, String environmentId, Object content, Duration timeToLive) {
        return Completable.create(emitter -> {
            if (!places.tryAcquire()) {
                if (executor.isShutdown()) {
                    emitter.onError(shutDown());
                } else {
                    refused.incrementAndGet();
                    emitter.onError(new RejectedExecutionException("too many approval commands are waiting to be written"));
                }
                return;
            }
            // Whichever of the write and the disposal claims it first: a write that started keeps its place until the
            // repository returns, since disposing does not stop its thread; one that did not start leaves the queue.
            AtomicBoolean claimed = new AtomicBoolean();
            Runnable write = () -> {
                if (!claimed.compareAndSet(false, true)) {
                    return;
                }
                Throwable failure = null;
                try {
                    commandRepository.create(command(tag, organizationId, environmentId, content, timeToLive));
                } catch (Throwable e) {
                    // Throwable: a repository plugin whose classloader is closed throws a LinkageError, and the call must still be told.
                    failed.incrementAndGet();
                    failure = e;
                } finally {
                    // Before the caller is told, so that a caller told of the end of its write never finds the place taken.
                    places.release();
                }
                if (failure == null) {
                    emitter.onComplete();
                } else {
                    emitter.tryOnError(failure);
                }
            };
            try {
                executor.execute(write);
            } catch (RejectedExecutionException e) {
                // The writer was shut down after the place was taken.
                places.release();
                emitter.onError(shutDown());
                return;
            }
            emitter.setCancellable(() -> {
                if (claimed.compareAndSet(false, true)) {
                    executor.remove(write);
                    places.release();
                }
            });
        });
    }

    private Command command(CommandTags tag, String organizationId, String environmentId, Object content, Duration timeToLive)
        throws JsonProcessingException {
        Command command = new Command();
        command.setFrom(node.id());
        command.setTo(MessageRecipient.MANAGEMENT_APIS.name());
        command.setTags(List.of(tag.name()));
        command.setOrganizationId(organizationId);
        command.setEnvironmentId(environmentId);
        command.setContent(objectMapper.writeValueAsString(content));
        command.setExpiredAt(Date.from(clock.instant().plus(timeToLive)));
        return command;
    }

    private static RejectedExecutionException shutDown() {
        return new RejectedExecutionException("the approval command writer is shut down");
    }
}
