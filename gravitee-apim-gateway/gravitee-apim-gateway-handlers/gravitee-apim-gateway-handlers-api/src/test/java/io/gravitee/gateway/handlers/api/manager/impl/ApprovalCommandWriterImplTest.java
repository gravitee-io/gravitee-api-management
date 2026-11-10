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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.reactivex.rxjava3.observers.TestObserver;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
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

    @AfterEach
    void shutDown() {
        writer.shutdown();
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

    /** As the sync executors do: a burst of writes leaves no thread parked for good once it is over. */
    @Test
    void should_let_idle_writer_threads_end() {
        ThreadPoolExecutor pool = ApprovalCommandWriterImpl.writerPool(2);
        try {
            assertThat(pool.allowsCoreThreadTimeOut()).isTrue();
            assertThat(pool.getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(15L);
        } finally {
            pool.shutdownNow();
        }
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

    @Nested
    class Bounded {

        private final Semaphore inRepository = new Semaphore(0);
        private CountDownLatch release;
        private ThreadPoolExecutor pool;
        private ApprovalCommandWriterImpl small;

        @BeforeEach
        void blockTheRepository() throws Exception {
            release = new CountDownLatch(1);
            lenient()
                .when(commandRepository.create(any()))
                .thenAnswer(invocation -> {
                    inRepository.release();
                    release.await(5, TimeUnit.SECONDS);
                    return invocation.getArgument(0);
                });
            pool = ApprovalCommandWriterImpl.writerPool(1);
            small = new ApprovalCommandWriterImpl(commandRepository, objectMapper, node, Clock.fixed(NOW, ZoneOffset.UTC), pool, 2);
        }

        @AfterEach
        void letGo() throws InterruptedException {
            release.countDown();
            small.shutdown();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        @Test
        void should_refuse_a_write_beyond_the_limit_instead_of_queueing_it() {
            TestObserver<Void> running = small.send(request()).test();
            TestObserver<Void> waiting = small.send(request()).test();

            small.send(request()).test().assertError(RejectedExecutionException.class);

            release.countDown();
            running.awaitDone(5, TimeUnit.SECONDS).assertComplete();
            waiting.awaitDone(5, TimeUnit.SECONDS).assertComplete();
        }

        @Test
        void should_free_the_place_of_a_write_whose_caller_gave_up_before_it_started() throws Exception {
            small.send(request()).test();
            TestObserver<Void> gaveUp = small.send(request()).test();
            small.send(request()).test().assertError(RejectedExecutionException.class);

            gaveUp.dispose();

            TestObserver<Void> next = small.send(request()).test();
            release.countDown();
            next.awaitDone(5, TimeUnit.SECONDS).assertComplete();
            verify(commandRepository, times(2)).create(any());
        }

        /** A write already in the repository cannot be taken back: its place stays taken until the repository returns. */
        @Test
        void should_keep_the_place_of_a_running_write_whose_caller_gave_up() throws Exception {
            TestObserver<Void> gaveUp = small.send(request()).test();
            assertThat(inRepository.tryAcquire(5, TimeUnit.SECONDS)).isTrue();
            gaveUp.dispose();
            TestObserver<Void> queued = small.send(request()).test();

            small.send(request()).test().assertError(RejectedExecutionException.class);

            release.countDown();
            queued.awaitDone(5, TimeUnit.SECONDS).assertComplete();
            assertThat(small.waiting()).isZero();
        }

        @Test
        void should_free_the_place_of_a_write_that_completed_before_telling_its_caller() throws Exception {
            TestObserver<Void> first = small.send(request()).test();
            TestObserver<Void> second = small.send(request()).test();
            release.countDown();
            first.awaitDone(5, TimeUnit.SECONDS).assertComplete();
            second.awaitDone(5, TimeUnit.SECONDS).assertComplete();

            assertThat(small.waiting()).isZero();
            small.send(request()).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        }

        @Test
        void should_free_the_place_of_a_write_that_failed() throws Exception {
            doThrow(new TechnicalException("repository unavailable")).when(commandRepository).create(any());
            small.send(request()).test().awaitDone(5, TimeUnit.SECONDS).assertError(TechnicalException.class);
            small.send(request()).test().awaitDone(5, TimeUnit.SECONDS).assertError(TechnicalException.class);

            small.send(request()).test().awaitDone(5, TimeUnit.SECONDS).assertError(TechnicalException.class);
            assertThat(small.waiting()).isZero();
        }

        @Test
        void should_refuse_every_write_once_shut_down_without_counting_it_as_too_many() {
            SimpleMeterRegistry registry = new SimpleMeterRegistry();
            small.bindTo(registry);
            small.shutdown();

            small.send(request()).test().assertError(ApprovalCommandWriterImplTest::isShutDownRefusal);

            assertThat(registry.get("human_approval_commands_refused").functionCounter().count()).isZero();
            assertThat(small.waiting()).isZero();
        }

        @Test
        void should_tell_a_write_refused_while_the_writer_is_full_and_shut_down_that_it_is_shut_down() {
            SimpleMeterRegistry registry = new SimpleMeterRegistry();
            small.bindTo(registry);
            small.send(request()).test();
            small.send(request()).test();
            small.shutdown();

            small.send(request()).test().assertError(ApprovalCommandWriterImplTest::isShutDownRefusal);

            assertThat(registry.get("human_approval_commands_refused").functionCounter().count()).isZero();
        }

        /** The pool can stop between the moment a write takes its place and the moment the write is handed to it. */
        @Test
        void should_fail_a_write_the_pool_no_longer_takes_and_free_its_place() {
            pool.shutdown();

            small.send(request()).test().awaitDone(5, TimeUnit.SECONDS).assertError(ApprovalCommandWriterImplTest::isShutDownRefusal);

            assertThat(small.waiting()).isZero();
        }

        @Test
        void should_count_the_writes_waiting_the_refused_ones_and_the_failed_ones() throws Exception {
            SimpleMeterRegistry registry = new SimpleMeterRegistry();
            small.bindTo(registry);
            TestObserver<Void> first = small.send(request()).test();
            TestObserver<Void> second = small.send(request()).test();
            small.send(request()).test().assertError(RejectedExecutionException.class);

            assertThat(registry.get("human_approval_commands_waiting").gauge().value()).isEqualTo(2);
            assertThat(registry.get("human_approval_commands_refused").functionCounter().count()).isEqualTo(1);

            release.countDown();
            first.awaitDone(5, TimeUnit.SECONDS).assertComplete();
            second.awaitDone(5, TimeUnit.SECONDS).assertComplete();
            assertThat(registry.get("human_approval_commands_waiting").gauge().value()).isZero();
            doThrow(new TechnicalException("repository unavailable")).when(commandRepository).create(any());
            small.send(request()).test().awaitDone(5, TimeUnit.SECONDS).assertError(TechnicalException.class);
            assertThat(registry.get("human_approval_commands_failed").functionCounter().count()).isEqualTo(1);
        }
    }

    private static boolean isShutDownRefusal(Throwable error) {
        return error instanceof RejectedExecutionException && error.getMessage().contains("shut down");
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
