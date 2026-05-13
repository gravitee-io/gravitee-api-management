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
package io.gravitee.gateway.reactive.handlers.api.processor.validation;

import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
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
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1)
@State(Scope.Benchmark)
public class NullByteRequestProcessorBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(NullByteRequestProcessorBenchmark.class.getSimpleName()).forks(1).build();
        new Runner(opt).run();
    }

    @Param({ "1", "5", "20", "50" })
    private int paramCount;

    private MultiValueMap<String, String> cleanParams;
    private MultiValueMap<String, String> dirtyParams;
    private String cleanUri;
    private String dirtyUri;

    @Setup
    public void setup() {
        cleanParams = build(paramCount, false);
        dirtyParams = build(paramCount, true);
        cleanUri = buildUri(paramCount, false);
        dirtyUri = buildUri(paramCount, true);
    }

    @Benchmark
    public boolean scan_uri_clean() {
        return (NullByteRequestProcessor.containsEncodedNullByte(cleanUri) || NullByteRequestProcessor.containsNullByte(cleanUri));
    }

    @Benchmark
    public boolean scan_uri_dirty() {
        return (NullByteRequestProcessor.containsEncodedNullByte(dirtyUri) || NullByteRequestProcessor.containsNullByte(dirtyUri));
    }

    @Benchmark
    public void scan_clean_params(Blackhole bh) {
        boolean dirty = false;
        for (var entry : cleanParams.entrySet()) {
            if (NullByteRequestProcessor.containsNullByte(entry.getKey())) {
                dirty = true;
                break;
            }
            for (String value : entry.getValue()) {
                if (NullByteRequestProcessor.containsNullByte(value)) {
                    dirty = true;
                    break;
                }
            }
        }
        bh.consume(dirty);
    }

    @Benchmark
    public void scan_dirty_params(Blackhole bh) {
        boolean dirty = false;
        for (var entry : dirtyParams.entrySet()) {
            if (NullByteRequestProcessor.containsNullByte(entry.getKey())) {
                dirty = true;
                break;
            }
            for (String value : entry.getValue()) {
                if (NullByteRequestProcessor.containsNullByte(value)) {
                    dirty = true;
                    break;
                }
            }
        }
        bh.consume(dirty);
    }

    @Benchmark
    public void baseline_iterate_only(Blackhole bh) {
        for (var entry : cleanParams.entrySet()) {
            bh.consume(entry.getKey());
            for (String value : entry.getValue()) {
                bh.consume(value);
            }
        }
    }

    private static MultiValueMap<String, String> build(int count, boolean injectNullByteLast) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        for (int i = 0; i < count; i++) {
            params.add("param" + i, "value" + i);
        }
        if (injectNullByteLast && count > 0) {
            params.add("evil", "payload\u0000");
        }
        return params;
    }

    private static String buildUri(int count, boolean injectNullByteLast) {
        StringBuilder sb = new StringBuilder("/api/test?");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append('&');
            }
            sb.append("param").append(i).append("=value").append(i);
        }
        if (injectNullByteLast && count > 0) {
            sb.append("&evil=payload%00");
        }
        return sb.toString();
    }
}
