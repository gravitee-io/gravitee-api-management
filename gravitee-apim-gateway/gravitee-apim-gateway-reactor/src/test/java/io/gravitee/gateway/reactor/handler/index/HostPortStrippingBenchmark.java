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
package io.gravitee.gateway.reactor.handler.index;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Isolates the port stripping that {@code OverlappingHttpAcceptor.matchHost} performs on every acceptor
 * of every request, and measures the three ways of writing it.
 *
 * <p>The claim under test is that {@code String.replaceAll} recompiles its pattern on each call, because
 * the pattern is held as a {@code String} rather than as a {@code Pattern}. The comparison against a
 * precompiled pattern separates the cost of compiling from the cost of matching; the manual variant shows
 * what is left once the regex goes away entirely.
 *
 * @author GraviteeSource Team
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class HostPortStrippingBenchmark {

    private static final String PORT_AS_STRING = ":(\\d{1,5})$";
    private static final Pattern PORT_AS_PATTERN = Pattern.compile(PORT_AS_STRING);

    private static final int MAX_PORT_DIGITS = 5;

    @Param({ "api.acme.com", "api.acme.com:8082" })
    public String host;

    /**
     * What the gateway runs today.
     */
    @Benchmark
    public String string_replace_all() {
        return host.replaceAll(PORT_AS_STRING, "");
    }

    /**
     * The same regex, compiled once.
     */
    @Benchmark
    public String precompiled_pattern() {
        return PORT_AS_PATTERN.matcher(host).replaceAll("");
    }

    /**
     * No regex at all, which is what the index does.
     */
    @Benchmark
    public String manual_scan() {
        return OverlappingHttpAcceptorIndex.stripPort(host);
    }

    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder().include(HostPortStrippingBenchmark.class.getSimpleName()).build()).run();
    }
}
