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
package io.gravitee.gateway.reactive.handlers.api.processor.subscription;

import static io.gravitee.gateway.api.ExecutionContext.ATTR_APPLICATION;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_PLAN;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_SUBSCRIPTION_ID;
import static io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor.REMOTE_ADDRESS_HASHES_CACHE;
import static io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessorTest.APPLICATION_ID;
import static io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessorTest.PLAN_ID;
import static io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessorTest.REMOTE_ADDRESS;
import static io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessorTest.TRANSACTION_ID;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.node.plugin.cache.standalone.StandaloneCacheManager;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.vertx.core.MultiMap;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 2)
public class SubscriptionProcessorBenchmark {

    private static SubscriptionProcessor cut;
    private static StandaloneCacheManager cacheManager;
    private DefaultExecutionContext executionContext;

    // used to run benchmark directly from IDE
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(SubscriptionProcessor.class.getSimpleName()).forks(1).build();

        new Runner(opt).run();
    }

    @Setup(Level.Trial)
    public void setUp() {
        HttpHeaders requestHeaders = HttpHeaders.create();
        HttpHeaders responseHeaders = HttpHeaders.create();
        MutableRequest mockRequest = mock(MutableRequest.class);
        MutableResponse mockResponse = mock(MutableResponse.class);
        lenient().when(mockRequest.headers()).thenReturn(requestHeaders);
        lenient().when(mockResponse.headers()).thenReturn(responseHeaders);
        lenient().when(mockRequest.remoteAddress()).thenReturn(REMOTE_ADDRESS);
        lenient().when(mockRequest.transactionId()).thenReturn(TRANSACTION_ID);

        VertxHttpHeaders requestParams = new VertxHttpHeaders(MultiMap.caseInsensitiveMultiMap());
        lenient().when(mockRequest.parameters()).thenReturn(requestParams);

        executionContext = new DefaultExecutionContext(mockRequest, mockResponse);
        executionContext.setAttribute(ATTR_PLAN, PLAN_ID);
        executionContext.setAttribute(ATTR_APPLICATION, APPLICATION_ID);
        executionContext.setAttribute(ATTR_SUBSCRIPTION_ID, REMOTE_ADDRESS);
        executionContext.metrics(Metrics.builder().timestamp(System.currentTimeMillis()).build());

        cacheManager = new StandaloneCacheManager();
        cut = SubscriptionProcessor.instance(null, cacheManager);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        cacheManager.destroy(REMOTE_ADDRESS_HASHES_CACHE);
    }

    @Benchmark
    public void bench_hash_computation() {
        try {
            cut.execute(executionContext).blockingAwait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
