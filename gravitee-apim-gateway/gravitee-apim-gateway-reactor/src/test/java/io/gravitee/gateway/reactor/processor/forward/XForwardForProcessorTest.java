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
package io.gravitee.gateway.reactor.processor.forward;

import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.http.HttpHeaderNames;
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

public class XForwardForProcessorTest {

    private MutableExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private HttpHeaders headers;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new SimpleExecutionContext(request, response);
        Mockito.when(request.headers()).thenReturn(headers);
        Metrics metrics = Metrics.on(System.currentTimeMillis()).build();
        when(request.metrics()).thenReturn(metrics);
    }

    @Test
    public void test_not_X_Forward_for_in_Header() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.getFirst(HttpHeaderNames.X_FORWARDED_FOR)).thenReturn(null);
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(
                context -> {
                    Assert.assertFalse(context.request() instanceof XForwardForRequest);
                    Assert.assertEquals("192.168.0.1", context.request().remoteAddress());
                    Assert.assertNull(context.request().metrics().getRemoteAddress());
                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_with_one_X_Forward_for_in_Header() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.getFirst(HttpHeaderNames.X_FORWARDED_FOR)).thenReturn("197.225.30.74");
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(
                context -> {
                    Assert.assertTrue(context.request() instanceof XForwardForRequest);
                    Assert.assertEquals("197.225.30.74", context.request().remoteAddress());
                    Assert.assertEquals("197.225.30.74", context.request().metrics().getRemoteAddress());
                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_with_one_X_Forward_for_in_Header_withPort() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.getFirst(HttpHeaderNames.X_FORWARDED_FOR)).thenReturn("197.225.30.74:5000");
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(
                context -> {
                    Assert.assertTrue(context.request() instanceof XForwardForRequest);
                    Assert.assertEquals("197.225.30.74", context.request().remoteAddress());
                    Assert.assertEquals("197.225.30.74", context.request().metrics().getRemoteAddress());
                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_with_many_X_Forward_for_in_Header() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.getFirst(HttpHeaderNames.X_FORWARDED_FOR)).thenReturn("197.225.30.74, 10.0.0.1, 10.0.0.2");
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(
                context -> {
                    Assert.assertTrue(context.request() instanceof XForwardForRequest);
                    Assert.assertEquals("197.225.30.74", context.request().remoteAddress());
                    Assert.assertEquals("197.225.30.74", context.request().metrics().getRemoteAddress());
                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_with_many_X_Forward_for_in_Header_withPorts() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.getFirst(HttpHeaderNames.X_FORWARDED_FOR)).thenReturn("197.225.30.74:5000, 10.0.0.1, 10.0.0.2");
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(
                context -> {
                    Assert.assertTrue(context.request() instanceof XForwardForRequest);
                    Assert.assertEquals("197.225.30.74", context.request().remoteAddress());
                    Assert.assertEquals("197.225.30.74", context.request().metrics().getRemoteAddress());
                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_with_one_X_Forward_for_in_Header_withIPv6() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.getFirst(HttpHeaderNames.X_FORWARDED_FOR)).thenReturn("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(
                context -> {
                    Assert.assertTrue(context.request() instanceof XForwardForRequest);
                    Assert.assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", context.request().remoteAddress());
                    Assert.assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", context.request().metrics().getRemoteAddress());
                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_with_one_X_Forward_for_in_Header_withIPv6_hexadecimalFormat() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.getFirst(HttpHeaderNames.X_FORWARDED_FOR)).thenReturn("2001:db8:85a3:0:0:8a2e:370:7334");
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(
                context -> {
                    Assert.assertTrue(context.request() instanceof XForwardForRequest);
                    Assert.assertEquals("2001:db8:85a3:0:0:8a2e:370:7334", context.request().remoteAddress());
                    Assert.assertEquals("2001:db8:85a3:0:0:8a2e:370:7334", context.request().metrics().getRemoteAddress());
                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_with_one_X_Forward_for_in_Header_withIPv6_hexadecimalFormat_consecutiveColons() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.getFirst(HttpHeaderNames.X_FORWARDED_FOR)).thenReturn("2001:db8:85a3::8a2e:370:7334");
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(
                context -> {
                    Assert.assertTrue(context.request() instanceof XForwardForRequest);
                    Assert.assertEquals("2001:db8:85a3::8a2e:370:7334", context.request().remoteAddress());
                    Assert.assertEquals("2001:db8:85a3::8a2e:370:7334", context.request().metrics().getRemoteAddress());
                    lock.countDown();
                }
            )
            .handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }
}
