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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.definition.approval.ApprovalCaller;
import io.gravitee.gamma.definition.approval.ApprovalOutcomeCommand;
import io.gravitee.gamma.definition.approval.ApprovalRequestCommand;
import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.model.Command;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalCommandWriterImplTest {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CommandRepository commandRepository;

    @Mock
    private Node node;

    private ApprovalCommandWriterImpl writer;

    @BeforeEach
    void setUp() {
        lenient().when(node.id()).thenReturn("node-1");
        writer = new ApprovalCommandWriterImpl(commandRepository, objectMapper, node, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void should_write_a_request_for_the_management_api() throws Exception {
        ApprovalRequestCommand request = request();

        writer.send(request).test().await().assertComplete();

        Command command = written();
        assertThat(command.getFrom()).isEqualTo("node-1");
        assertThat(command.getTo()).isEqualTo("MANAGEMENT_APIS");
        assertThat(command.getTags()).containsExactly("APPROVAL_REQUEST");
        assertThat(command.getEnvironmentId()).isEqualTo("env-1");
        assertThat(command.getOrganizationId()).isEqualTo("org-1");
        assertThat(command.getExpiredAt()).isEqualTo(Date.from(NOW.plusSeconds(600)));
        assertThat(objectMapper.readValue(command.getContent(), ApprovalRequestCommand.class)).isEqualTo(request);
    }

    @Test
    void should_write_an_outcome_for_the_management_api() throws Exception {
        ApprovalOutcomeCommand outcome = new ApprovalOutcomeCommand(
            "req-1",
            "org-1",
            "env-1",
            "node-1",
            "lease-1",
            "EXECUTED",
            "2026-10-31T10:00:00Z"
        );

        writer.send(outcome).test().await().assertComplete();

        Command command = written();
        assertThat(command.getFrom()).isEqualTo("node-1");
        assertThat(command.getTo()).isEqualTo("MANAGEMENT_APIS");
        assertThat(command.getTags()).containsExactly("APPROVAL_OUTCOME");
        assertThat(command.getEnvironmentId()).isEqualTo("env-1");
        assertThat(command.getOrganizationId()).isEqualTo("org-1");
        assertThat(command.getExpiredAt()).isEqualTo(Date.from(NOW.plusSeconds(3_600)));
        assertThat(objectMapper.readValue(command.getContent(), ApprovalOutcomeCommand.class)).isEqualTo(outcome);
    }

    @ParameterizedTest
    @ValueSource(strings = { "STARTED", "ADDED_BY_A_NEWER_POLICY" })
    void should_write_any_outcome_as_given(String outcome) throws Exception {
        writer
            .send(new ApprovalOutcomeCommand("req-1", "org-1", "env-1", "node-1", "lease-1", outcome, "2026-10-31T10:00:00Z"))
            .test()
            .await()
            .assertComplete();

        assertThat(objectMapper.readTree(written().getContent()).get("outcome").asText()).isEqualTo(outcome);
    }

    @Test
    void should_write_off_the_calling_thread_on_a_bounded_pool() throws Exception {
        AtomicReference<String> writingThread = new AtomicReference<>();
        when(commandRepository.create(any())).thenAnswer(invocation -> {
            writingThread.set(Thread.currentThread().getName());
            return invocation.getArgument(0);
        });

        writer.send(request()).test().await().assertComplete();

        assertThat(writingThread.get()).isNotEqualTo(Thread.currentThread().getName()).startsWith("gio.approval-command-writer-");
    }

    /**
     * {@code Schedulers.io()} is an unbounded cached pool: nothing stops a burst of held calls from spawning one thread
     * per write. This asserts the pool is actually bounded, not just named — many more concurrent writes than the pool
     * size must still be served by no more distinct threads than the pool allows.
     */
    @Test
    void should_bound_the_number_of_writer_threads_under_a_burst() throws Exception {
        int callCount = ApprovalCommandWriterImpl.WRITER_POOL_SIZE * 8;
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(callCount);
        Set<String> threadsUsed = ConcurrentHashMap.newKeySet();
        when(commandRepository.create(any())).thenAnswer(invocation -> {
            threadsUsed.add(Thread.currentThread().getName());
            release.await(5, TimeUnit.SECONDS);
            return invocation.getArgument(0);
        });

        IntStream.range(0, callCount).forEach(i -> writer.send(request()).subscribe(completed::countDown, error -> completed.countDown()));
        release.countDown();

        assertThat(completed.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(threadsUsed).hasSizeLessThanOrEqualTo(ApprovalCommandWriterImpl.WRITER_POOL_SIZE);
    }

    @Test
    void should_not_write_before_subscription() throws Exception {
        writer.send(request());

        verify(commandRepository, never()).create(any());
    }

    @Test
    void should_report_a_failed_write_to_the_caller() throws Exception {
        when(commandRepository.create(any())).thenThrow(new TechnicalException("repository unavailable"));

        writer.send(request()).test().await().assertError(TechnicalException.class);
    }

    private Command written() throws TechnicalException {
        ArgumentCaptor<Command> captor = ArgumentCaptor.forClass(Command.class);
        verify(commandRepository).create(captor.capture());
        return captor.getValue();
    }

    private ApprovalRequestCommand request() {
        return ApprovalRequestCommand.builder()
            .approvalRequestId("req-1")
            .kind("OPEN")
            .organizationId("org-1")
            .environmentId("env-1")
            .apiId("api-1")
            .ruleId("rule-1")
            .ruleUpdatedAt("2026-10-01T00:00:00Z")
            .toolName("refund_order")
            .arguments(objectMapper.createObjectNode().put("amount", 120))
            .argumentsDigest("digest-1")
            .caller(new ApprovalCaller("app-1", "sub-1"))
            .deliveryPath("MRTR")
            .gatewayNodeId("node-1")
            .sentAt("2026-10-31T10:00:00Z")
            .build();
    }
}
