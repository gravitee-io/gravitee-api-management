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
import java.util.Random;
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
 * What {@code http.pathHandling} costs on a traffic profile rather than on a single path.
 *
 * <p>A benchmark that hands the same string over and over lets the branch predictor learn it and
 * reports a number no production gateway will ever see. This one cycles a thousand distinct paths,
 * mixed at 95% ordinary and 5% carrying dot segments, which is generous towards the attack: real
 * traffic carrying five percent of traversals would be an incident in itself.
 *
 * <p>This is the figure to quote when someone asks what the setting costs.
 *
 * @author GraviteeSource Team
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 2)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class RequestPathHandlingCorpusBenchmark {

    private static final int CORPUS_SIZE = 1_000;
    private static final int TRAVERSAL_PERCENTAGE = 5;
    private static final long SEED = 20260814L;

    @Param({ "RAW", "REJECT", "NORMALIZE" })
    public String mode;

    private RequestPathHandling handling;
    private String[] corpus;
    private int cursor;

    // Run directly from the IDE.
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(RequestPathHandlingCorpusBenchmark.class.getSimpleName()).build();
        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        handling = RequestPathHandling.valueOf(mode);
        corpus = buildCorpus();
        cursor = 0;
    }

    /**
     * Fixed seed: the corpus is the same on every run, so two runs remain comparable.
     */
    private String[] buildCorpus() {
        final Random random = new Random(SEED);
        final String[] paths = new String[CORPUS_SIZE];

        for (int i = 0; i < CORPUS_SIZE; i++) {
            final String customer = Integer.toHexString(random.nextInt(0xFFFF));
            final int order = random.nextInt(100_000);

            if (random.nextInt(100) < TRAVERSAL_PERCENTAGE) {
                paths[i] = random.nextBoolean()
                    ? "/v1/customers/" + customer + "/../../../admin/orders/" + order
                    : "/v1/customers/" + customer + "/%2e%2e/%2e%2e/admin/orders/" + order;
            } else {
                paths[i] = "/v1/customers/" + customer + "/orders/" + order + "/details";
            }
        }
        return paths;
    }

    /**
     * @return the path the dispatcher carries forward, or {@code null} when the request is rejected.
     */
    @Benchmark
    public String path_handling_decision() {
        // Wrapped by hand rather than with a modulo: RAW runs at over a billion operations a
        // second, so a monotonic counter overflows int within a single iteration and the modulo of
        // a negative is negative.
        int next = cursor + 1;
        if (next == CORPUS_SIZE) {
            next = 0;
        }
        cursor = next;
        final String rawPath = corpus[next];

        if (handling == RequestPathHandling.RAW) {
            return rawPath;
        }
        if (!RequestPathNormalizer.needsNormalization(rawPath)) {
            return rawPath;
        }
        return handling == RequestPathHandling.REJECT ? null : RequestPathNormalizer.normalize(rawPath);
    }
}
