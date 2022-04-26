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
package io.gravitee.gateway.flow.benchmark;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.http2.HttpFrame;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.ws.WebSocket;
import io.gravitee.gateway.core.condition.CompositeConditionEvaluator;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.flow.BestMatchFlowResolver;
import io.gravitee.gateway.flow.FlowResolver;
import io.gravitee.gateway.flow.condition.ConditionalFlowResolver;
import io.gravitee.gateway.flow.condition.evaluation.PathBasedConditionEvaluator;
import io.gravitee.reporter.api.http.Metrics;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.net.ssl.SSLSession;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BestMatchFlowResolverBenchmark {

    FlowResolver flowResolver;
    SimpleExecutionContext executionContext;

    private final ConditionEvaluator evaluator = new CompositeConditionEvaluator(new PathBasedConditionEvaluator());

    // used to run benchmark directly from IDE
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(BestMatchFlowResolverBenchmark.class.getSimpleName()).forks(1).build();

        new Runner(opt).run();
    }

    @Setup
    public void setUp() {
        flowResolver = new TestFlowResolver(evaluator, buildFlows());
        executionContext =
            new SimpleExecutionContext(new TestRequest("/book/99/chapter/888/page/7777/paragraph/6666/line/5/char/10"), null);
    }

    @Benchmark
    public void benchOldBestMatch() {
        new OldBestMatchFlowResolver(flowResolver).resolve(executionContext);
    }

    @Benchmark
    public void benchBestMatch() {
        new BestMatchFlowResolver(flowResolver).resolve(executionContext);
    }

    private List<Flow> buildFlows() {
        return List
            .of(
                "/book",
                "/book/99",
                "/book/:id",
                "/book/99/chapter/888",
                "/book/:id/chapter/:chapterId",
                "/book/99/chapter/888",
                "/book/:id/chapter/:chapterId/page/:pageId",
                "/book/99/chapter/888/page/7777",
                "/book/:id/chapter/:chapterId/page/:pageId/paragraph/:paragraphId",
                "/book/99/chapter/888/page/7777/paragraph/6666",
                "/book/:id/chapter/:chapterId/page/:pageId/paragraph/:paragraphId/line/:lineId",
                "/book/99/chapter/888/page/7777/paragraph/6666/line/5",
                "/book/:id/chapter/:chapterId/page/:pageId/paragraph/:paragraphId/line/:lineId/char/:charid",
                "/book/99/chapter/888/page/7777/paragraph/6666/line/5/char/1"
            )
            .stream()
            .map(
                path -> {
                    Flow flow = new Flow();
                    PathOperator pathOperator = new PathOperator();
                    pathOperator.setPath(path);
                    // No need to test different operator in this test.
                    // Input of BestMatchPolicyResolver is already filtered by PathBasedConditionEvaluator
                    pathOperator.setOperator(Operator.STARTS_WITH);
                    flow.setPathOperator(pathOperator);
                    return flow;
                }
            )
            .collect(Collectors.toList());
    }

    private static class TestFlowResolver extends ConditionalFlowResolver {

        private List<Flow> flows;

        public TestFlowResolver(ConditionEvaluator evaluator, List<Flow> flows) {
            super(evaluator);
            this.flows = flows;
        }

        @Override
        protected List<Flow> resolve0(ExecutionContext context) {
            return flows;
        }
    }

    private class TestRequest implements Request {

        private String path;

        public TestRequest(String path) {
            this.path = path;
        }

        @Override
        public String id() {
            return null;
        }

        @Override
        public String transactionId() {
            return null;
        }

        @Override
        public String uri() {
            return null;
        }

        @Override
        public String path() {
            return null;
        }

        @Override
        public String pathInfo() {
            return path;
        }

        @Override
        public String contextPath() {
            return null;
        }

        @Override
        public MultiValueMap<String, String> parameters() {
            return null;
        }

        @Override
        public MultiValueMap<String, String> pathParameters() {
            return null;
        }

        @Override
        public HttpHeaders headers() {
            return null;
        }

        @Override
        public HttpMethod method() {
            return null;
        }

        @Override
        public String scheme() {
            return null;
        }

        @Override
        public HttpVersion version() {
            return null;
        }

        @Override
        public long timestamp() {
            return 0;
        }

        @Override
        public String remoteAddress() {
            return null;
        }

        @Override
        public String localAddress() {
            return null;
        }

        @Override
        public SSLSession sslSession() {
            return null;
        }

        @Override
        public Metrics metrics() {
            return null;
        }

        @Override
        public boolean ended() {
            return false;
        }

        @Override
        public Request timeoutHandler(Handler<Long> handler) {
            return null;
        }

        @Override
        public Handler<Long> timeoutHandler() {
            return null;
        }

        @Override
        public boolean isWebSocket() {
            return false;
        }

        @Override
        public WebSocket websocket() {
            return null;
        }

        @Override
        public Request customFrameHandler(Handler<HttpFrame> handler) {
            return null;
        }

        @Override
        public String host() {
            return null;
        }

        @Override
        public Request closeHandler(Handler<Void> closeHandler) {
            return null;
        }

        @Override
        public ReadStream<Buffer> bodyHandler(Handler<Buffer> handler) {
            return null;
        }

        @Override
        public ReadStream<Buffer> endHandler(Handler<Void> handler) {
            return null;
        }
    }
}
