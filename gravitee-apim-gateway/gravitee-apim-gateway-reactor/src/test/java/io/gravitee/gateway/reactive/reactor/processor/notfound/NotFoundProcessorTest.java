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
package io.gravitee.gateway.reactive.reactor.processor.notfound;

import static io.gravitee.gateway.reactive.reactor.processor.notfound.NotFoundProcessor.UNKNOWN_SERVICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.reactor.processor.AbstractProcessorTest;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotFoundProcessorTest extends AbstractProcessorTest {

    private NotFoundProcessor notFoundProcessor;
    private StandardEnvironment standardEnvironment;

    @BeforeEach
    public void beforeEach() {
        standardEnvironment = new StandardEnvironment();
        when(mockResponse.headers()).thenReturn(HttpHeaders.create());
        when(mockResponse.end(ctx)).thenReturn(Completable.complete());
        notFoundProcessor = new NotFoundProcessor(standardEnvironment);
    }

    @Test
    void should_end_response_with_404() {
        notFoundProcessor.execute(ctx).test().assertResult();
        verify(mockResponse).status(HttpStatusCode.NOT_FOUND_404);
        verify(mockResponse, times(2)).headers();
        verify(mockResponse).body(any(Buffer.class));
        verify(mockResponse).end(ctx);

        Metrics metrics = ctx.metrics();
        assertThat(metrics.getApiId()).isEqualTo(UNKNOWN_SERVICE);
        assertThat(metrics.getApiName()).isEqualTo(UNKNOWN_SERVICE);
        assertThat(metrics.getApplicationId()).isEqualTo(UNKNOWN_SERVICE);
        verify(mockResponse).status(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    void should_end_response_with_404_and_set_logs() {
        standardEnvironment.getSystemProperties().put("handlers.notfound.log.enabled", "true");
        notFoundProcessor.execute(ctx).test().assertResult();
        verify(mockResponse).status(HttpStatusCode.NOT_FOUND_404);
        verify(mockResponse, times(2)).headers();
        verify(mockResponse).body(any(Buffer.class));
        verify(mockResponse).end(ctx);

        Metrics metrics = ctx.metrics();
        assertThat(metrics.getApiId()).isEqualTo(UNKNOWN_SERVICE);
        assertThat(metrics.getApiName()).isEqualTo(UNKNOWN_SERVICE);
        assertThat(metrics.getApplicationId()).isEqualTo(UNKNOWN_SERVICE);
        assertThat(metrics.getLog()).isNotNull();
        assertThat(metrics.getLog().getApiName()).isEqualTo(UNKNOWN_SERVICE);
        assertThat(metrics.getLog().getApiId()).isEqualTo(UNKNOWN_SERVICE);
        verify(mockResponse).status(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    void should_end_response_with_404_and_not_set_logs() {
        standardEnvironment.getSystemProperties().put("handlers.notfound.log.enabled", "false");
        notFoundProcessor.execute(ctx).test().assertResult();
        verify(mockResponse).status(HttpStatusCode.NOT_FOUND_404);
        verify(mockResponse, times(2)).headers();
        verify(mockResponse).body(any(Buffer.class));
        verify(mockResponse).end(ctx);

        Metrics metrics = ctx.metrics();
        assertThat(metrics.getApiId()).isEqualTo(UNKNOWN_SERVICE);
        assertThat(metrics.getApiName()).isEqualTo(UNKNOWN_SERVICE);
        assertThat(metrics.getApplicationId()).isEqualTo(UNKNOWN_SERVICE);
        assertThat(metrics.getLog()).isNull();
        verify(mockResponse).status(HttpStatusCode.NOT_FOUND_404);
    }
}
