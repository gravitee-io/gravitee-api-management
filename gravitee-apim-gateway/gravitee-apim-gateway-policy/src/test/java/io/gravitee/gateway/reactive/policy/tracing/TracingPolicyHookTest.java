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
package io.gravitee.gateway.reactive.policy.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpRequest;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.api.opentelemetry.internal.InternalRequest;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TracingPolicyHookTest {

    @Mock
    private HttpExecutionContext ctx;

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private HttpRequest request;

    @Mock
    private HttpHeaders httpHeaders;

    private TracingPolicyHook hook;

    @BeforeEach
    void setUp() {
        hook = new TracingPolicyHook();
    }

    @Test
    void shouldHaveCorrectId() {
        assertThat(hook.id()).isEqualTo("hook-tracing-policy");
    }

    @Test
    void shouldExecutePreHookWithoutVerbose() {
        String policyId = "test-policy";
        when(ctx.getTracer()).thenReturn(tracer);
        when(tracer.startSpanFrom(any(InternalRequest.class))).thenReturn(span);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_TRACING_VERBOSE_ENABLED)).thenReturn(false);

        hook.pre(policyId, ctx, ExecutionPhase.REQUEST).test().assertResult();
        verify(tracer).startSpanFrom(any(InternalRequest.class));
        verify(span, never()).addEvent(anyString(), anyMap());
    }

    @Test
    void shouldExecutePreHookWithVerbose() {
        String policyId = "test-policy";
        Map<String, String> singleValueMap = Map.of("content-type", "application/json");

        when(ctx.getTracer()).thenReturn(tracer);
        when(tracer.startSpanFrom(any(InternalRequest.class))).thenReturn(span);
        when(ctx.getInternalAttribute("tracing-span-test-policy")).thenReturn(span);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_TRACING_VERBOSE_ENABLED)).thenReturn(true);
        when(ctx.request()).thenReturn(request);
        when(request.headers()).thenReturn(httpHeaders);
        when(httpHeaders.toSingleValueMap()).thenReturn(singleValueMap);
        when(ctx.getAttributeNames()).thenReturn(Set.of("api.id"));
        when(ctx.getAttribute("api.id")).thenReturn("test-api");

        hook.pre(policyId, ctx, ExecutionPhase.REQUEST).test().assertResult();
        verify(tracer).startSpanFrom(any(InternalRequest.class));
        verify(span).addEvent(eq("gravitee.policy.pre"), anyMap());
    }

    @Test
    void shouldExecutePostHookWithTriggerAttributes() {
        String policyId = "test-policy";
        String condition = "{#request.headers['x-test'] != null}";

        when(ctx.getInternalAttribute("tracing-span-test-policy")).thenReturn(span);
        when(ctx.getInternalAttribute("gravitee.policy.trigger.condition." + policyId)).thenReturn(condition);
        lenient().when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_TRACING_VERBOSE_ENABLED)).thenReturn(false);
        when(ctx.getTracer()).thenReturn(tracer);

        hook.post(policyId, ctx, ExecutionPhase.REQUEST).test().assertResult();
        verify(span).withAttribute("gravitee.policy.trigger.condition", condition);
        verify(span).withAttribute("gravitee.policy.trigger.executed", "true");
        verify(tracer).end(span);
    }

    @Test
    void shouldExecutePostHookWithVerbose() {
        String policyId = "test-policy";
        Map<String, String> headers = Map.of("header1", "value1", "header2", "value2");

        when(ctx.getInternalAttribute("tracing-span-test-policy")).thenReturn(span);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_TRACING_VERBOSE_ENABLED)).thenReturn(true);
        lenient().when(ctx.getInternalAttribute("gravitee.policy.trigger.condition." + policyId)).thenReturn(null);
        when(ctx.request()).thenReturn(request);
        when(request.headers()).thenReturn(httpHeaders);
        when(httpHeaders.toSingleValueMap()).thenReturn(headers);
        when(ctx.getAttributeNames()).thenReturn(Set.of("api.id"));
        when(ctx.getAttribute("api.id")).thenReturn("test-api");
        when(ctx.getTracer()).thenReturn(tracer);

        hook.post(policyId, ctx, ExecutionPhase.REQUEST).test().assertResult();
        verify(span).addEvent(eq("gravitee.policy.post"), anyMap());
        verify(tracer).end(span);
    }

    @Test
    void shouldNotAddTriggerConditionWhenBlank() {
        String policyId = "test-policy";

        when(ctx.getInternalAttribute("tracing-span-test-policy")).thenReturn(span);
        when(ctx.getInternalAttribute("gravitee.policy.trigger.condition." + policyId)).thenReturn("");
        lenient().when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_TRACING_VERBOSE_ENABLED)).thenReturn(false);
        when(ctx.getTracer()).thenReturn(tracer);
        hook.post(policyId, ctx, ExecutionPhase.REQUEST).test().assertResult();
        verify(span, never()).withAttribute(eq("gravitee.policy.trigger.condition"), anyString());
        verify(span).withAttribute("gravitee.policy.trigger.executed", "true");
    }

    @Test
    void shouldCaptureAllHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("host", "localhost");
        headers.put("authorization", "Bearer secret-token-12345");
        headers.put("x-api-key", "secret-api-key-12345");
        headers.put("content-type", "application/json");

        Map<String, String> captured = hook.captureHeaders(headers);

        assertEquals("hook-tracing-policy", hook.id());
        assertEquals(4, captured.size());
        assertEquals("localhost", captured.get("host"));
        assertEquals("Bearer secret-token-12345", captured.get("authorization"));
        assertEquals("secret-api-key-12345", captured.get("x-api-key"));
        assertEquals("application/json", captured.get("content-type"));
    }

    @Test
    void shouldHandleNullOrEmptyHeaders() {
        Map<String, String> capturedFromNull = hook.captureHeaders(null);
        assertNotNull(capturedFromNull);
        assertTrue(capturedFromNull.isEmpty());

        Map<String, String> capturedFromEmpty = hook.captureHeaders(new HashMap<>());
        assertNotNull(capturedFromEmpty);
        assertTrue(capturedFromEmpty.isEmpty());
    }

    @Test
    void shouldSkipNullHeaderValues() {
        Map<String, String> headers = new HashMap<>();
        headers.put("valid-header", "value");
        headers.put("null-header", null);

        Map<String, String> captured = hook.captureHeaders(headers);

        assertEquals(1, captured.size());
        assertEquals("value", captured.get("valid-header"));
        assertFalse(captured.containsKey("null-header"));
    }

    @Test
    void shouldPreserveHeaderOrder() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("header-1", "value-1");
        headers.put("header-2", "value-2");
        headers.put("header-3", "value-3");

        Map<String, String> captured = hook.captureHeaders(headers);
        List<String> keys = new ArrayList<>(captured.keySet());

        assertEquals("header-1", keys.get(0));
        assertEquals("header-2", keys.get(1));
        assertEquals("header-3", keys.get(2));
    }

    @Test
    void shouldHandleDifferentTypeOfHeaders() {
        String longValue = "x".repeat(1000);
        Map<String, String> headers = Map.of(
            "Content-Type",
            "application/json",
            "special-header",
            "value with spaces & special @#$ chars",
            "long-header",
            longValue
        );

        Map<String, String> captured = hook.captureHeaders(headers);

        assertEquals("application/json", captured.get("Content-Type"));
        assertEquals("value with spaces & special @#$ chars", captured.get("special-header"));
        assertEquals(longValue, captured.get("long-header"));
    }

    @Test
    void shouldCaptureAllContextAttributes() {
        when(ctx.getAttributeNames()).thenReturn(Set.of("user.password", "api.id", "app.secret", "user.id"));
        when(ctx.getAttribute("user.password")).thenReturn("secret123");
        when(ctx.getAttribute("api.id")).thenReturn("abc-123");
        when(ctx.getAttribute("app.secret")).thenReturn("my-secret");
        when(ctx.getAttribute("user.id")).thenReturn("user123");

        Map<String, String> captured = hook.captureContextAttributes(ctx);

        assertEquals("hook-tracing-policy", hook.id());
        assertEquals(4, captured.size());
        assertEquals("secret123", captured.get("user.password"));
        assertEquals("abc-123", captured.get("api.id"));
        assertEquals("my-secret", captured.get("app.secret"));
        assertEquals("user123", captured.get("user.id"));
    }

    @Test
    void shouldHandleNullAttributeNames() {
        when(ctx.getAttributeNames()).thenReturn(null);

        Map<String, String> captured = hook.captureContextAttributes(ctx);

        assertNotNull(captured);
        assertTrue(captured.isEmpty());
    }

    @Test
    void shouldHandleEmptyAttributeNames() {
        when(ctx.getAttributeNames()).thenReturn(Collections.emptySet());

        Map<String, String> captured = hook.captureContextAttributes(ctx);

        assertNotNull(captured);
        assertTrue(captured.isEmpty());
    }

    @Test
    void shouldSkipNullAttributeValue() {
        when(ctx.getAttributeNames()).thenReturn(Set.of("null-attr", "valid-attr"));
        when(ctx.getAttribute("null-attr")).thenReturn(null);
        when(ctx.getAttribute("valid-attr")).thenReturn("value");

        Map<String, String> captured = hook.captureContextAttributes(ctx);

        assertEquals(1, captured.size());
        assertFalse(captured.containsKey("null-attr"));
        assertEquals("value", captured.get("valid-attr"));
    }

    @Test
    void shouldHandleDifferentTypeOfAttributes() {
        String longValue = "y".repeat(1000);
        when(ctx.getAttribute("obj-attr")).thenReturn(new Object());
        when(ctx.getAttributeNames()).thenReturn(Set.of("int-attr", "bool-attr", "obj-attr", "long-attr"));
        when(ctx.getAttribute("int-attr")).thenReturn(123);
        when(ctx.getAttribute("bool-attr")).thenReturn(true);
        when(ctx.getAttribute("obj-attr")).thenReturn(new Object());
        when(ctx.getAttribute("long-attr")).thenReturn(longValue);

        Map<String, String> captured = hook.captureContextAttributes(ctx);

        assertEquals("123", captured.get("int-attr"));
        assertEquals("true", captured.get("bool-attr"));
        assertNotNull(captured.get("obj-attr"));
        assertEquals(longValue, captured.get("long-attr"));
    }
}
