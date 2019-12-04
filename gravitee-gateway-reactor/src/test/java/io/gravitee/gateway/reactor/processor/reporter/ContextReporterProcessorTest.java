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
package io.gravitee.gateway.reactor.processor.reporter;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.context.ReportableExecutionContextWrapper;
import io.gravitee.reporter.api.http.Metrics;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Ovcharov Ilya (ovcharov.ilya@gmail.com)
 */
public class ContextReporterProcessorTest {

    public static final String GRAVITEE_ATTR_NAME = "gravitee.attribute.forTest";
    public static final String GRAVITEE_ATTR_VALUE = "gravitee_value";
    public static final String ANOTHER_ATTR_NAME = "another.attribute.forTest";
    public static final String ANOTHER_ATTR_VALUE = "another_value";

    private MutableExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private HttpHeaders headers;

    @Mock
    private ReporterService reporterService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new SimpleExecutionContext(request, response);
        context.setAttribute(GRAVITEE_ATTR_NAME, GRAVITEE_ATTR_VALUE);
        context.setAttribute(ANOTHER_ATTR_NAME, ANOTHER_ATTR_VALUE);
        Mockito.when(request.headers()).thenReturn(headers);
        Mockito.when(response.headers()).thenReturn(new HttpHeaders());
        Mockito.when(request.metrics()).thenReturn(Metrics.on(System.currentTimeMillis()).build());
    }

    @Test
    public void shouldReportReportableExecutionContextWrapper() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        doAnswer( invocation -> {
            ReportableExecutionContextWrapper contextWrapper = invocation.getArgument(0);
            Assert.assertTrue(contextWrapper instanceof ReportableExecutionContextWrapper);
            Assert.assertTrue(contextWrapper.getContext() instanceof ExecutionContext);
            Assert.assertNull(contextWrapper.getContext().getAttribute(GRAVITEE_ATTR_NAME));
            Assert.assertNotNull(contextWrapper.getContext().getAttribute(ANOTHER_ATTR_NAME));
            Assert.assertEquals(ANOTHER_ATTR_VALUE, contextWrapper.getContext().getAttribute(ANOTHER_ATTR_NAME));
            return null;
        }).when(reporterService)
          .report(any(Reportable.class));

        new ContextReporterProcessor(reporterService)
                .handler(context -> {
                    lock.countDown();
                }).handle(context);

        Assert.assertTrue(lock.await(10000, TimeUnit.MILLISECONDS));
    }

}
