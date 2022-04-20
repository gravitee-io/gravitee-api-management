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
package io.gravitee.gateway.handlers.api.processor.pathparameters;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http2.HttpFrame;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.ws.WebSocket;
import io.gravitee.gateway.handlers.api.path.Path;
import io.gravitee.gateway.handlers.api.path.impl.AbstractPathResolver;
import io.gravitee.reporter.api.http.Metrics;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSession;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2)
public class PathParametersProcessorBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(PathParametersProcessorBenchmark.class.getSimpleName()).forks(1).build();

        new Runner(opt).run();
    }

    private static final String PATH = "/store/:storeId/order/:orderId";
    private PathParametersIndexProcessor processorIndex;
    private PathResolver resolver = new PathResolver(PATH);

    @Setup
    public void setup() {
        processorIndex = new PathParametersIndexProcessor(resolver);
        processorIndex.handler(
            new Handler<ExecutionContext>() {
                @Override
                public void handle(ExecutionContext result) {
                    // Do nothing
                }
            }
        );
    }

    @Benchmark
    public void bench_processorIndex() {
        ExecutionContext context = new SimpleExecutionContext(new SimpleRequest("/store/myStore/order/190783"), null);
        processorIndex.handle(context);
    }

    static class PathResolver extends AbstractPathResolver {

        PathResolver(String sPath) {
            Path path = new Path();
            path.setPath(sPath);
            register(path);
        }
    }

    static class SimpleRequest implements Request {

        private final String path;
        private MultiValueMap<String, String> pathParameters;

        SimpleRequest(String path) {
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
            return this.path;
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
            if (pathParameters == null) {
                pathParameters = new LinkedMultiValueMap<>();
            }

            return pathParameters;
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
        public Request timeoutHandler(Handler<Long> timeoutHandler) {
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
        public Request customFrameHandler(Handler<HttpFrame> frameHandler) {
            return null;
        }

        @Override
        public Request closeHandler(Handler<Void> closeHandler) {
            return null;
        }

        @Override
        public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
            return null;
        }

        @Override
        public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
            return null;
        }
    }
}
