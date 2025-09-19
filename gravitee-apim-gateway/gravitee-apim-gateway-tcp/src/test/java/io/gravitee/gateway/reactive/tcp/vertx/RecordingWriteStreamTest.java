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
package io.gravitee.gateway.reactive.tcp.vertx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RecordingWriteStreamTest {

    RecordingWriteStream cut = new RecordingWriteStream();

    @Test
    void should_write_buffer() {
        Future<Void> f = cut.write(Buffer.buffer("hey"));
        waitCompleted(f, null);
        assertThat(cut.getRecordedBuffers()).containsExactly("hey");
        assertThat(f.succeeded()).isTrue();
    }

    @Test
    void should_write_buffer_with_handler() {
        // works because write is not asynchronous
        cut.write(Buffer.buffer("hey"), b -> {
            assertThat(b.succeeded()).isTrue();
        });
        assertThat(cut.getRecordedBuffers()).containsExactly("hey");
    }

    @Test
    void should_fail_write_buffer() {
        IllegalStateException err = new IllegalStateException("failed");
        cut.errOnNextWrite(err);
        Future<Void> f = cut.write(Buffer.buffer("hey"));
        waitCompleted(f, err);
        f.onComplete(empty -> {
            assertThat(cut.getRecordedBuffers()).isEmpty();
            assertThat(f.cause()).isEqualTo(err);
        });
        assertThat(f.failed()).isTrue();
    }

    @Test
    void should_fail_write_buffer_with_handler() {
        IllegalStateException err = new IllegalStateException("failed");
        cut.errOnNextWrite(err);
        AtomicBoolean exec = new AtomicBoolean();
        cut.write(Buffer.buffer("hey"), r -> {
            assertThat(r.cause()).isEqualTo(err);
            assertThat(r.failed()).isTrue();
            assertThat(cut.getRecordedBuffers()).isEmpty();
            exec.set(true);
        });
        // works because write is synchronous
        assertThat(exec.get()).isTrue();
    }

    @Test
    void should_record_buffers_concurrently() {
        var letters = List.of("A", "B", "C", "D", "E", "F", "G");
        var numbers = List.of("0", "1", "2", "3", "4", "5", "6");

        Flowable<Buffer> lettersFlowable = Flowable.fromIterable(letters)
            .map(Buffer::buffer)
            .doOnNext(b -> cut.write(b));
        Flowable<Buffer> numbersFlowable = Flowable.fromIterable(numbers)
            .map(Buffer::buffer)
            .doOnNext(b -> cut.write(b));
        Flowable.mergeArray(lettersFlowable, numbersFlowable).test().assertValueCount(letters.size() + numbers.size()).assertComplete();
        var all = new ArrayList<>(letters);
        all.addAll(numbers);
        assertThat(cut.getRecordedBuffers()).containsAnyElementsOf(all);
    }

    private static void waitCompleted(Future<Void> f, Throwable err) {
        try {
            f.toCompletionStage().toCompletableFuture().get(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
            fail("failed waiting for future to complete", e);
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isEqualTo(err);
        }
    }
}
