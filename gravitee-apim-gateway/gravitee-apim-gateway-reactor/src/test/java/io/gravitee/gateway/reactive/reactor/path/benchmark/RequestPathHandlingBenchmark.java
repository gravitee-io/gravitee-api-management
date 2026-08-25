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

import io.gravitee.common.http.RequestPathNormalizer;
import io.gravitee.gateway.env.RequestPathHandling;
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
 * What each value of {@code http.pathHandling} costs per request, by shape of path.
 *
 * <p>The measured method mirrors the decision {@code DefaultHttpRequestDispatcher} takes on every
 * request, before the acceptor is resolved: under {@code RAW} the path is handed over untouched,
 * under {@code REJECT} it is normalized and compared, under {@code NORMALIZE} the normalized value
 * is the one carried forward. Everything after that decision — acceptor resolution, plans, flows —
 * is identical in all three and is deliberately out of the measurement.
 *
 * <p>{@code RAW} is the baseline: it never calls the normalizer, so its numbers are the cost of the
 * feature switched off, and the floor of this harness.
 *
 * <p>Throughput, in operations per second, is what translates into capacity for an operator, and it
 * is the only figure reported: for a deterministic single-threaded call, average time is its
 * inverse and measuring both would double the run for nothing. A single constant string per shape
 * is easy on the branch predictor, which is why {@link RequestPathHandlingCorpusBenchmark} exists
 * next to this one.
 *
 * @author GraviteeSource Team
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 2)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class RequestPathHandlingBenchmark {

    private static final Map<String, String> PATHS = Map.of(
        "ordinary",
        "/v1/orders/list",
        "ordinary_deep",
        "/api/v2/customers/8f3a/orders/2026/08/13/items/447/details",
        "ordinary_uuids",
        "/v1/customers/3f2504e0-4f89-11d3-9a0c-0305e82c3301/orders/7c9e6679-7425-40de-944b-e07fc1f90ae7/items/0f8fad5b-d9cb-469f-a165-70867728950e",
        "ordinary_with_dot",
        "/v1/orders/12345.json",
        "ordinary_encoded",
        "/v1/orders/a%20b/details",
        "dot_segments",
        "/alpha/api/../../beta/api/echo",
        "encoded_dot_segments",
        "/alpha/api/%2e%2e/%2e%2e/beta/api/echo"
    );

    @Param({ "RAW", "REJECT", "NORMALIZE" })
    public String mode;

    @Param(
        { "ordinary", "ordinary_deep", "ordinary_uuids", "ordinary_with_dot", "ordinary_encoded", "dot_segments", "encoded_dot_segments" }
    )
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
        if (!RequestPathNormalizer.needsNormalization(rawPath)) {
            return rawPath;
        }
        return handling == RequestPathHandling.REJECT ? null : RequestPathNormalizer.normalize(rawPath);
    }
}
