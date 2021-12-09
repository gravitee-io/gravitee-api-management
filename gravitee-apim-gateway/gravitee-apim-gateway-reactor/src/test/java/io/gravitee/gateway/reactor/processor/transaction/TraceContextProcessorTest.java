/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactor.processor.transaction;

import static io.gravitee.gateway.reactor.processor.transaction.TraceContextProcessor.HEADER_TRACE_PARENT;
import static io.gravitee.gateway.reactor.processor.transaction.TraceContextProcessor.HEADER_TRACE_STATE;

import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.reporter.api.http.Metrics;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TraceContextProcessorTest {

    private MutableExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new SimpleExecutionContext(request, response);
        Mockito.when(request.headers()).thenReturn(HttpHeaders.create());
        Mockito.when(response.headers()).thenReturn(HttpHeaders.create());
        Mockito.when(request.metrics()).thenReturn(Metrics.on(System.currentTimeMillis()).build());
    }

    @Test
    public void shouldPropagateSameTraceparent() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        final String traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

        request.headers().set(HEADER_TRACE_STATE, "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7");
        request.headers().set(HEADER_TRACE_PARENT, traceparent);
        new TraceContextProcessor()
            .handler(
                context -> {
                    Assert.assertEquals(traceparent, context.request().headers().getFirst(HEADER_TRACE_PARENT));
                    Assert.assertEquals(traceparent, context.response().headers().getFirst(HEADER_TRACE_PARENT));
                    Assert.assertEquals(
                        "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7",
                        context.request().headers().getFirst(TraceContextProcessor.HEADER_TRACE_STATE)
                    );

                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldNotPropagate_Tracestate_IfInvalidTraceParent() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        final String traceparent = "00-00000000000000000000000000000000-00f067aa0ba902b7-01";

        request.headers().set(HEADER_TRACE_STATE, "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7");
        request.headers().set(HEADER_TRACE_PARENT, traceparent);

        new TraceContextProcessor()
            .handler(
                context -> {
                    Assert.assertNotEquals(traceparent, context.request().headers().getFirst(HEADER_TRACE_PARENT));
                    Assert.assertNotEquals(traceparent, context.response().headers().getFirst(HEADER_TRACE_PARENT));

                    Assert.assertNull(context.request().headers().getFirst(HEADER_TRACE_STATE));
                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldNotPropagateSameTraceparent_withInvalidVersion() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        final String traceparent = "ff-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

        request.headers().set(HEADER_TRACE_STATE, "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7");
        request.headers().set(HEADER_TRACE_PARENT, traceparent);

        new TraceContextProcessor()
            .handler(
                context -> {
                    Assert.assertNotEquals(traceparent, context.request().headers().getFirst(HEADER_TRACE_PARENT));
                    Assert.assertNotEquals(traceparent, context.response().headers().getFirst(HEADER_TRACE_PARENT));

                    Assert.assertNull(context.request().headers().getFirst(HEADER_TRACE_STATE));
                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldNotPropagateSameTraceparent_withInvalidTraceId_All0() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        final String traceparent = "00-00000000000000000000000000000000-00f067aa0ba902b7-01";

        request.headers().set(HEADER_TRACE_STATE, "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7");
        request.headers().set(HEADER_TRACE_PARENT, traceparent);

        new TraceContextProcessor()
            .handler(
                context -> {
                    Assert.assertNotEquals(traceparent, context.request().headers().getFirst(HEADER_TRACE_PARENT));
                    Assert.assertNotEquals(traceparent, context.response().headers().getFirst(HEADER_TRACE_PARENT));

                    Assert.assertNull(context.request().headers().getFirst(HEADER_TRACE_STATE));
                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldNotPropagateSameTraceparent_withInvalidTraceId_Length() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        final String traceparent = "00-00f067aa0ba902b7-00f067aa0ba902b7-01";

        request.headers().set(HEADER_TRACE_STATE, "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7");
        request.headers().set(HEADER_TRACE_PARENT, traceparent);

        new TraceContextProcessor()
            .handler(
                context -> {
                    Assert.assertNotEquals(traceparent, context.request().headers().getFirst(HEADER_TRACE_PARENT));
                    Assert.assertNotEquals(traceparent, context.response().headers().getFirst(HEADER_TRACE_PARENT));

                    Assert.assertNull(context.request().headers().getFirst(HEADER_TRACE_STATE));
                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldNotPropagateSameTraceparent_InvalidParentId() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        final String traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-0000000000000000-01";

        request.headers().set(HEADER_TRACE_STATE, "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7");
        request.headers().set(HEADER_TRACE_PARENT, traceparent);

        new TraceContextProcessor()
            .handler(
                context -> {
                    Assert.assertNotEquals(traceparent, context.request().headers().getFirst(HEADER_TRACE_PARENT));
                    Assert.assertNotEquals(traceparent, context.response().headers().getFirst(HEADER_TRACE_PARENT));

                    Assert.assertNull(context.request().headers().getFirst(HEADER_TRACE_STATE));
                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldHaveTraceparent() throws InterruptedException {
        final String traceparent_regex = "(([0-9a-e][0-9a-f])|([0-9a-f][0-9a-e]))-([0-9a-f]{32})-([0-9a-f]{16})-([0-9a-f]{2})";
        final CountDownLatch lock = new CountDownLatch(1);

        Mockito.when(request.id()).thenReturn(UUID.toString(UUID.random()));

        request.headers().set(TraceContextProcessor.HEADER_TRACE_STATE, "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7");
        new TraceContextProcessor()
            .handler(
                context -> {
                    Assert.assertTrue(context.request().headers().getFirst(HEADER_TRACE_PARENT).matches(traceparent_regex));
                    Assert.assertTrue(context.response().headers().getFirst(HEADER_TRACE_PARENT).matches(traceparent_regex));

                    // if TRACESTATE provided witout traceparent, tracestate is removed
                    Assert.assertNull(context.request().headers().getFirst(HEADER_TRACE_STATE));
                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }
}
