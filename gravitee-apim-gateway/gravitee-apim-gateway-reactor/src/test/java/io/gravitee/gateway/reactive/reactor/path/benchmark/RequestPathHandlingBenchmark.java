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
package io.gravitee.gateway.reactive.reactor.path.benchmark;

import io.gravitee.gateway.env.RequestPathHandling;
import io.gravitee.gateway.reactive.reactor.path.RequestPathNormalizer;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * What each value of {@code http.pathHandling} costs per request.
 *
 * <p>The measured method mirrors the decision {@code DefaultHttpRequestDispatcher} takes on every
 * request, before the acceptor is resolved: under {@code RAW} the path is handed over untouched,
 * under {@code REJECT} it is normalized and compared, under {@code NORMALIZE} the normalized value
 * is the one carried forward. Everything after that decision — acceptor resolution, plans, flows —
 * is identical in all three and is deliberately out of the measurement.
 *
 * <p>Each mode is crossed with the shapes of path a gateway actually receives, because the cost is
 * driven by the shape far more than by the mode: an ordinary path exits on the first scan, only a
 * path carrying dot segments pays for the resolution.
 *
 * @author GraviteeSource Team
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
public class RequestPathHandlingBenchmark {

    private static final Map<String, String> PATHS = Map.of(
        "ordinary",
        "/v1/orders/list",
        "ordinary_deep",
        "/api/v2/customers/8f3a/orders/2026/08/13/items/447/details",
        "ordinary_with_dot",
        "/v1/orders/12345.json",
        "dot_segments",
        "/alpha/api/../../beta/api/echo",
        "encoded_dot_segments",
        "/alpha/api/%2e%2e/%2e%2e/beta/api/echo"
    );

    @Param({ "RAW", "REJECT", "NORMALIZE" })
    public String mode;

    @Param({ "ordinary", "ordinary_deep", "ordinary_with_dot", "dot_segments", "encoded_dot_segments" })
    public String shape;

    private RequestPathHandling handling;
    private String rawPath;

    // Run directly from the IDE.
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(RequestPathHandlingBenchmark.class.getSimpleName()).build();
        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        handling = RequestPathHandling.valueOf(mode);
        rawPath = PATHS.get(shape);
    }

    /**
     * @return the path the dispatcher carries forward, or {@code null} when the request is rejected.
     */
    @Benchmark
    public String path_handling_decision() {
        if (handling == RequestPathHandling.RAW) {
            return rawPath;
        }
        final String normalized = RequestPathNormalizer.normalize(rawPath);
        if (normalized != rawPath && handling == RequestPathHandling.REJECT) {
            return null;
        }
        return normalized;
    }
}
