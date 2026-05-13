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
package io.gravitee.gateway.reactive.handlers.api.processor.validation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.reactive.handlers.api.processor.AbstractProcessorTest;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NullByteRequestProcessorTest extends AbstractProcessorTest {

    private NullByteRequestProcessor processor;

    @BeforeEach
    public void beforeEach() {
        processor = NullByteRequestProcessor.instance();
        lenient().when(mockRequest.uri()).thenReturn("/api/test");
        lenient().when(mockRequest.transactionId()).thenReturn("tx-test");
    }

    @Test
    public void should_complete_when_request_is_clean() {
        when(mockRequest.uri()).thenReturn("/api/test?prompt=cool&lang=en");
        spyRequestHeaders.add("X-Custom-Header", "value");

        processor.execute(spyCtx).test().assertResult();
    }

    @Test
    public void should_complete_when_uri_has_no_query_string() {
        when(mockRequest.uri()).thenReturn("/api/test");

        processor.execute(spyCtx).test().assertResult();
    }

    @Test
    public void should_reject_with_400_when_encoded_null_byte_in_query_value() {
        when(mockRequest.uri()).thenReturn("/api/test?prompt=bastard%00");

        TestObserver<Void> observer = processor.execute(spyCtx).test();
        assertInterruptedWithNullByteFailure(observer);
    }

    @Test
    public void should_reject_with_400_when_encoded_null_byte_in_query_key() {
        when(mockRequest.uri()).thenReturn("/api/test?prompt=cool&prompt%00=bastard");

        TestObserver<Void> observer = processor.execute(spyCtx).test();
        assertInterruptedWithNullByteFailure(observer);
    }

    @Test
    public void should_reject_with_400_when_literal_null_byte_in_uri() {
        when(mockRequest.uri()).thenReturn("/api/test?prompt=bastard\u0000");

        TestObserver<Void> observer = processor.execute(spyCtx).test();
        assertInterruptedWithNullByteFailure(observer);
    }

    @Test
    public void should_reject_with_400_when_null_byte_in_header_value() {
        spyRequestHeaders.add("X-Custom-Header", "value\u0000injected");

        TestObserver<Void> observer = processor.execute(spyCtx).test();
        assertInterruptedWithNullByteFailure(observer);
    }

    @Test
    public void should_reject_with_400_when_encoded_null_byte_in_header_value() {
        spyRequestHeaders.add("Cookie", "session=abc%00def");

        TestObserver<Void> observer = processor.execute(spyCtx).test();
        assertInterruptedWithNullByteFailure(observer);
    }

    @Test
    public void should_reject_with_400_when_null_byte_in_header_name() {
        spyRequestHeaders.add("X-Custom\u0000Header", "value");

        TestObserver<Void> observer = processor.execute(spyCtx).test();
        assertInterruptedWithNullByteFailure(observer);
    }

    @Test
    public void should_handle_null_uri_without_failing() {
        when(mockRequest.uri()).thenReturn(null);

        processor.execute(spyCtx).test().assertResult();
    }

    @Test
    public void should_detect_uri_null_byte_before_header() {
        when(mockRequest.uri()).thenReturn("/api/test?prompt=bad%00");
        spyRequestHeaders.add("X-Bad", "also\u0000bad");

        NullByteRequestProcessor.Detection detection = processor.scan(mockRequest);
        assertThat(detection).isNotNull();
        assertThat(detection.source()).isEqualTo(NullByteRequestProcessor.Source.URI);
    }

    @Test
    public void contains_null_byte_helper_should_handle_nulls_and_empty() {
        assertThat(NullByteRequestProcessor.containsNullByte(null)).isFalse();
        assertThat(NullByteRequestProcessor.containsNullByte("")).isFalse();
        assertThat(NullByteRequestProcessor.containsNullByte("clean")).isFalse();
        assertThat(NullByteRequestProcessor.containsNullByte("dir\u0000ty")).isTrue();
        assertThat(NullByteRequestProcessor.containsNullByte("\u0000")).isTrue();
    }

    @Test
    public void contains_encoded_null_byte_helper_should_detect_only_percent_zero_zero() {
        assertThat(NullByteRequestProcessor.containsEncodedNullByte(null)).isFalse();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("")).isFalse();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("clean")).isFalse();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("%2F%3F")).isFalse();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("%0")).isFalse();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("%00")).isTrue();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("prefix%00suffix")).isTrue();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("%%00")).isTrue();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("%01%00")).isTrue();
    }

    @Test
    public void contains_encoded_null_byte_helper_should_not_throw_on_truncated_percent_at_tail() {
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("a%0")).isFalse();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("ab%0")).isFalse();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("/api/test?p=%0")).isFalse();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("a%")).isFalse();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("ab%")).isFalse();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("/api/test?p=%")).isFalse();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("/api/test?p=%2")).isFalse();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("%%%")).isFalse();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("a%00")).isTrue();
        assertThat(NullByteRequestProcessor.containsEncodedNullByte("/api/test?p=%00")).isTrue();
    }

    @Test
    public void should_have_stable_id() {
        assertThat(processor.getId()).isEqualTo(NullByteRequestProcessor.ID);
    }

    private void assertInterruptedWithNullByteFailure(TestObserver<Void> observer) {
        observer.assertError(InterruptionFailureException.class);
        observer.assertError(error -> {
            ExecutionFailure failure = ((InterruptionFailureException) error).getExecutionFailure();
            assertThat(failure.statusCode()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(failure.key()).isEqualTo(NullByteRequestProcessor.NULL_BYTE_REJECTED_KEY);
            assertThat(failure.message()).isEqualTo(NullByteRequestProcessor.NULL_BYTE_REJECTED_MESSAGE);
            return true;
        });
    }
}
