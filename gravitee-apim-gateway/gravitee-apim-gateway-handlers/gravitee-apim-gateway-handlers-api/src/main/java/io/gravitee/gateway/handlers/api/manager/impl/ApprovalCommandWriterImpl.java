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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.definition.approval.ApprovalOutcomeCommand;
import io.gravitee.gamma.definition.approval.ApprovalRequestCommand;
import io.gravitee.gateway.handlers.api.manager.ApprovalCommandWriter;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.CommandTags;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.management.model.MessageRecipient;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Writes the approval commands through the command repository, which is the bridge client on a hybrid gateway, so the
 * gateway reaches the control plane the way it already does for subscription failures.
 *
 * <p>Writes run on their own fixed-size pool, not {@code Schedulers.io()}: that one is an unbounded cached pool, and a
 * burst of held calls writing at once would otherwise spawn one thread per write. {@link #WRITER_POOL_SIZE} follows the
 * same sizing this module already uses for a bounded worker pool (available processors × 2, see
 * {@code SyncConfiguration.POOL_SIZE} and {@code ApiManagerImpl.PARALLELISM}).
 */
public class ApprovalCommandWriterImpl implements ApprovalCommandWriter {

    static final int WRITER_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    private static final Duration REQUEST_TIME_TO_LIVE = Duration.ofMinutes(10);
    private static final Duration OUTCOME_TIME_TO_LIVE = Duration.ofHours(1);

    private final CommandRepository commandRepository;
    private final ObjectMapper objectMapper;
    private final Node node;
    private final Clock clock;
    private final Scheduler writer;

    public ApprovalCommandWriterImpl(CommandRepository commandRepository, ObjectMapper objectMapper, Node node, Clock clock) {
        this.commandRepository = commandRepository;
        this.objectMapper = objectMapper;
        this.node = node;
        this.clock = clock;
        this.writer = Schedulers.from(writerExecutor());
    }

    private static ExecutorService writerExecutor() {
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "gio.approval-command-writer-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(WRITER_POOL_SIZE, threadFactory);
    }

    @Override
    public Completable send(ApprovalRequestCommand request) {
        return write(CommandTags.APPROVAL_REQUEST, request.getOrganizationId(), request.getEnvironmentId(), request, REQUEST_TIME_TO_LIVE);
    }

    @Override
    public Completable send(ApprovalOutcomeCommand outcome) {
        return write(CommandTags.APPROVAL_OUTCOME, outcome.getOrganizationId(), outcome.getEnvironmentId(), outcome, OUTCOME_TIME_TO_LIVE);
    }

    private Completable write(CommandTags tag, String organizationId, String environmentId, Object content, Duration timeToLive) {
        return Completable.fromAction(() -> {
            Command command = new Command();
            command.setFrom(node.id());
            command.setTo(MessageRecipient.MANAGEMENT_APIS.name());
            command.setTags(List.of(tag.name()));
            command.setOrganizationId(organizationId);
            command.setEnvironmentId(environmentId);
            command.setContent(objectMapper.writeValueAsString(content));
            command.setExpiredAt(Date.from(clock.instant().plus(timeToLive)));
            commandRepository.create(command);
        }).subscribeOn(writer);
    }
}
