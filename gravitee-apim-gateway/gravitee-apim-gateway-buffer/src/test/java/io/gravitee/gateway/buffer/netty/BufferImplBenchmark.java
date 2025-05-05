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
package io.gravitee.gateway.buffer.netty;

import io.gravitee.gateway.api.buffer.Buffer;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 5, time = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1)
@Warmup(iterations = 2, time = 3)
@State(Scope.Benchmark)
public class BufferImplBenchmark {

    private static final int count = 1000;
    private static final String hello = "Hello";

    @Benchmark
    public void benchAppend() {
        Buffer buffer = new BufferImpl();

        for (int i = 0; i < count; i++) {
            buffer = buffer.appendString(hello);
        }

        buffer.toString();
    }

    @Benchmark
    public void benchAppendOld() {
        Buffer bufferOld = new BufferOldImpl();

        for (int i = 0; i < count; i++) {
            bufferOld = bufferOld.appendString(hello);
        }

        bufferOld.toString();
    }
}
