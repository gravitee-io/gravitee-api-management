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
package io.gravitee.gateway.reactor.processor.forward;

import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.reporter.api.http.Metrics;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class XForwardProcessorTest {

    private MutableExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private HttpHeaders headers;

    @BeforeEach
    public void setUp() throws Exception {
        context = new SimpleExecutionContext(request, response);
        Mockito.when(request.headers()).thenReturn(headers);
        Metrics metrics = Metrics.on(System.currentTimeMillis()).build();
        when(request.metrics()).thenReturn(metrics);
    }

    @Test
    public void test_not_X_Forward_for_in_Header() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.get("X-Forwarded-For")).thenReturn(null);
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(context -> {
                Assertions.assertFalse(context.request() instanceof XForwardForRequest);
                Assertions.assertEquals("192.168.0.1", context.request().remoteAddress());
                Assertions.assertNull(context.request().metrics().getRemoteAddress());
                lock.countDown();
            })
            .handle(context);

        Assertions.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_with_one_X_Forward_for_in_Header() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.get("X-Forwarded-For")).thenReturn("197.225.30.74");
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(context -> {
                Assertions.assertTrue(context.request() instanceof XForwardForRequest);
                Assertions.assertEquals("197.225.30.74", context.request().remoteAddress());
                Assertions.assertEquals("197.225.30.74", context.request().metrics().getRemoteAddress());
                lock.countDown();
            })
            .handle(context);

        Assertions.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_with_one_X_Forward_for_in_Header_withPort() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.get("X-Forwarded-For")).thenReturn("197.225.30.74:5000");
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(context -> {
                Assertions.assertTrue(context.request() instanceof XForwardForRequest);
                Assertions.assertEquals("197.225.30.74", context.request().remoteAddress());
                Assertions.assertEquals("197.225.30.74", context.request().metrics().getRemoteAddress());
                lock.countDown();
            })
            .handle(context);

        Assertions.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_with_many_X_Forward_for_in_Header() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.get("X-Forwarded-For")).thenReturn("197.225.30.74 ,10.0.0.1,  10.0.0.2");
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(context -> {
                Assertions.assertTrue(context.request() instanceof XForwardForRequest);
                Assertions.assertEquals("197.225.30.74", context.request().remoteAddress());
                Assertions.assertEquals("197.225.30.74", context.request().metrics().getRemoteAddress());
                lock.countDown();
            })
            .handle(context);

        Assertions.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_with_many_X_Forward_for_in_Header_withPorts() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.get("X-Forwarded-For")).thenReturn("197.225.30.74:5000, 10.0.0.1, 10.0.0.2");
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(context -> {
                Assertions.assertTrue(context.request() instanceof XForwardForRequest);
                Assertions.assertEquals("197.225.30.74", context.request().remoteAddress());
                Assertions.assertEquals("197.225.30.74", context.request().metrics().getRemoteAddress());
                lock.countDown();
            })
            .handle(context);

        Assertions.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_with_one_X_Forward_for_in_Header_withIPv6() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.get("X-Forwarded-For")).thenReturn("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(context -> {
                Assertions.assertTrue(context.request() instanceof XForwardForRequest);
                Assertions.assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", context.request().remoteAddress());
                Assertions.assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", context.request().metrics().getRemoteAddress());
                lock.countDown();
            })
            .handle(context);

        Assertions.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_with_one_X_Forward_for_in_Header_withIPv6_hexadecimalFormat() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.get("X-Forwarded-For")).thenReturn("2001:db8:85a3:0:0:8a2e:370:7334");
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(context -> {
                Assertions.assertTrue(context.request() instanceof XForwardForRequest);
                Assertions.assertEquals("2001:db8:85a3:0:0:8a2e:370:7334", context.request().remoteAddress());
                Assertions.assertEquals("2001:db8:85a3:0:0:8a2e:370:7334", context.request().metrics().getRemoteAddress());
                lock.countDown();
            })
            .handle(context);

        Assertions.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_with_one_X_Forward_for_in_Header_withIPv6_hexadecimalFormat_consecutiveColons() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(headers.get("X-Forwarded-For")).thenReturn("2001:db8:85a3::8a2e:370:7334");
        when(request.remoteAddress()).thenReturn("192.168.0.1");

        new XForwardForProcessor()
            .handler(context -> {
                Assertions.assertTrue(context.request() instanceof XForwardForRequest);
                Assertions.assertEquals("2001:db8:85a3::8a2e:370:7334", context.request().remoteAddress());
                Assertions.assertEquals("2001:db8:85a3::8a2e:370:7334", context.request().metrics().getRemoteAddress());
                lock.countDown();
            })
            .handle(context);

        Assertions.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }
}
