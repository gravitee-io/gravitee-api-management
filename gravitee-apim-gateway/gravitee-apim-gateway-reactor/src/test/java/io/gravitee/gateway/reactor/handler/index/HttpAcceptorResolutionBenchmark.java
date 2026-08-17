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

import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import java.util.Arrays;
import java.util.List;
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
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Measures the cost of electing an API, comparing the sorted-list scan the gateway runs today with the
 * index meant to replace it.
 *
 * <p>The dimension that matters is {@code apiCount}: the scan is expected to degrade linearly with it
 * while the index is expected to stay flat. {@code outcome} matters almost as much, because a request
 * that matches nothing is the one case where the scan can never exit early, and it is the shape a flood
 * of unknown paths takes.
 *
 * <p>{@code HIT_LATE} deliberately targets the acceptor the scan reaches last. That is not a contrived
 * worst case: acceptors with no virtual host sort after every other one, and within them by ascending
 * path, so an API deployed on a context path late in the alphabet lands there by construction.
 *
 * @author GraviteeSource Team
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 4, time = 1)
public class HttpAcceptorResolutionBenchmark {

    private static final long SEED = 20260817L;

    /**
     * The bindings the corpus draws from, ordered so the unbound case is tried first.
     */
    private static final List<String> SERVER_IDS = Arrays.asList(null, "server-1", "server-2", "server-9");

    @Param({ "100", "1000", "10000" })
    public int apiCount;

    @Param({ "DEFAULT", "OVERLAPPING" })
    public String mode;

    @Param({ "NO_HOST", "WILDCARD_HEAVY" })
    public String shape;

    @Param({ "HIT_LATE", "MISS" })
    public String outcome;

    private LinearHttpAcceptorResolver linear;
    private HttpAcceptorIndex index;

    private String host;
    private String path;
    private String serverId;

    @Setup
    public void setup() {
        HttpAcceptorCorpus.Mode corpusMode = HttpAcceptorCorpus.Mode.valueOf(mode);
        HttpAcceptorCorpus corpus = new HttpAcceptorCorpus(SEED, corpusMode, HttpAcceptorCorpus.Shape.valueOf(shape), apiCount);

        linear = new LinearHttpAcceptorResolver(corpus.acceptors());
        index = corpusMode.overlapping ? new OverlappingHttpAcceptorIndex() : new DefaultHttpAcceptorIndex();
        corpus.acceptors().forEach(index::add);

        if ("MISS".equals(outcome)) {
            host = "unknown.acme.com";
            path = "/never-declared/deeper/still";
            serverId = null;
        } else {
            selectLatestReachableAcceptor();
        }

        // A benchmark measuring the wrong thing is worse than no benchmark: check that both
        // implementations agree on this exact request, and that a hit really hits.
        HttpAcceptor scanned = linear.resolve(host, path, serverId);
        HttpAcceptor indexed = index.resolve(host, path, serverId);
        if ("MISS".equals(outcome) && scanned != null) {
            throw new IllegalStateException("the MISS request matched " + scanned);
        }
        if ("HIT_LATE".equals(outcome) && scanned == null) {
            throw new IllegalStateException("the HIT_LATE request matched nothing: host=" + host + " path=" + path);
        }
        if (scanned != indexed && (scanned == null || indexed == null || scanned.reactor() != indexed.reactor())) {
            throw new IllegalStateException("scan elected " + scanned + " but index elected " + indexed);
        }
    }

    /**
     * Walks the sorted list backwards for a request the scan can only answer by reaching that far.
     * Taking the very last acceptor is not enough: it may be bound to server ids the request does not
     * carry, or shadowed by a shorter context path sorted earlier, in which case the scan exits before
     * the end and the benchmark would quietly measure something else.
     */
    private void selectLatestReachableAcceptor() {
        List<HttpAcceptor> sorted = linear.sorted();
        for (int position = sorted.size() - 1; position >= 0; position--) {
            HttpAcceptor candidate = sorted.get(position);
            String candidateHost = requestHostFor(candidate);
            String candidatePath = candidate.path() + "resource";
            for (String candidateServerId : SERVER_IDS) {
                if (linear.resolve(candidateHost, candidatePath, candidateServerId) == candidate) {
                    host = candidateHost;
                    path = candidatePath;
                    serverId = candidateServerId;
                    return;
                }
            }
        }
        throw new IllegalStateException("no acceptor in the population can be reached by a request");
    }

    private static String requestHostFor(HttpAcceptor acceptor) {
        String declared = acceptor.host();
        if (declared == null) {
            return "localhost";
        }
        // A stored host starting with a dot can only come from a wildcard, which matches any prefix.
        return declared.startsWith(".") ? "sub" + declared : declared;
    }

    @Benchmark
    public HttpAcceptor sorted_list_scan() {
        return linear.resolve(host, path, serverId);
    }

    @Benchmark
    public HttpAcceptor trie_index() {
        return index.resolve(host, path, serverId);
    }

    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder().include(HttpAcceptorResolutionBenchmark.class.getSimpleName()).build()).run();
    }
}
