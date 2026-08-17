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

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.gateway.reactor.handler.HttpAcceptorFactory;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.http.AccessPointHttpAcceptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Generates a reproducible population of acceptors and the requests to throw at them.
 *
 * <p>The point is coverage, not realism: hosts that are null, exact, wildcarded or differently cased,
 * ports present and absent, context paths that are prefixes of one another, server bindings that
 * overlap and that do not, and access point composites holding zero, one or several hosts.
 *
 * <p>Two acceptors are never generated on the same host and path. That combination makes the current
 * comparator non-antisymmetric, so the sorted list it produces has no defined order and cannot serve as
 * a reference; it is pinned separately in {@code HttpAcceptorComparatorContractTest}.
 *
 * @author GraviteeSource Team
 */
final class HttpAcceptorCorpus {

    enum Mode {
        DEFAULT(false),
        OVERLAPPING(true);

        final boolean overlapping;

        Mode(boolean overlapping) {
            this.overlapping = overlapping;
        }
    }

    enum Shape {
        /** Every API on a context path only, the common self-managed layout. */
        NO_HOST,
        /** Every API behind its own virtual host. */
        VHOST,
        /** Four fifths on a context path only, one fifth behind a virtual host. */
        MIXED,
        /** Virtual hosts, half of them wildcarded. Only meaningful in overlapping mode. */
        WILDCARD_HEAVY,
    }

    record Request(String host, String path, String serverId) {}

    private static final List<String> SEGMENTS = List.of("store", "orders", "users", "v1", "v2", "a", "ab", "b");
    private static final List<List<String>> SERVER_IDS = new ArrayList<>();

    static {
        SERVER_IDS.add(null);
        SERVER_IDS.add(List.of());
        SERVER_IDS.add(List.of("server-1"));
        SERVER_IDS.add(List.of("server-1", "server-2"));
        SERVER_IDS.add(List.of("server-9"));
    }

    private static final List<String> SERVER_ID_QUERIES = new ArrayList<>();

    static {
        SERVER_ID_QUERIES.add(null);
        SERVER_ID_QUERIES.add("server-1");
        SERVER_ID_QUERIES.add("server-2");
        SERVER_ID_QUERIES.add("server-9");
    }

    private final long seed;
    private final Mode mode;
    private final Random random;
    private final HttpAcceptorFactory factory;
    private final EventManager eventManager = new EventManagerImpl();

    private final List<HttpAcceptor> acceptors = new ArrayList<>();
    private final List<Request> requests = new ArrayList<>();
    private final Set<String> declared = new HashSet<>();

    HttpAcceptorCorpus(long seed, Mode mode, Shape shape, int size) {
        this.seed = seed;
        this.mode = mode;
        this.random = new Random(seed);
        this.factory = new HttpAcceptorFactory(mode.overlapping);
        generate(shape, size);
        addDegenerateRequests();
    }

    long seed() {
        return seed;
    }

    Collection<HttpAcceptor> acceptors() {
        return acceptors;
    }

    List<Request> requests() {
        return requests;
    }

    private void generate(Shape shape, int size) {
        int created = 0;
        int attempts = 0;
        while (created < size && attempts < size * 10) {
            attempts++;
            String host = host(shape, created);
            String path = path(created);
            if (host != null && host.startsWith("*") && !mode.overlapping) {
                // A literal star has no meaning outside overlapping mode.
                continue;
            }
            if (!declared.add(key(host, path))) {
                continue;
            }
            List<String> serverIds = pick(SERVER_IDS);
            if (random.nextInt(5) == 0) {
                createComposite(host, path, serverIds);
            } else {
                acceptors.add(factory.create(host, path, reactor(created), serverIds));
            }
            recordRequests(host, path);
            created++;
        }
        if (created < size) {
            // Loudly, because a silently saturated corpus makes two populations of different sizes
            // identical, and any measurement taken across them reads as flat for the wrong reason.
            throw new IllegalStateException(
                "corpus saturated at " + created + " acceptors for a requested size of " + size + " in shape " + shape
            );
        }
    }

    /**
     * Reproduces what the reactors build for an API with no explicit virtual host: a composite holding
     * one inner acceptor per access point, or a single host-less one when there is no access point.
     *
     * <p>At most one access point here. A composite holding several is placed in the sorted list at the
     * position of its <em>first</em> host while accepting requests for all of them, so the scan does not
     * honour "most specific host wins" for the others and cannot serve as a reference. That defect is
     * pinned on its own in {@code HttpAcceptorResolutionDifferentialTest}.
     */
    private void createComposite(String host, String path, List<String> serverIds) {
        List<ReactableAccessPoint> accessPoints = new ArrayList<>();
        if (host != null) {
            accessPoints.add(accessPoint(host));
        }
        acceptors.add(
            new AccessPointHttpAcceptor(
                eventManager,
                factory,
                "environment-" + acceptors.size(),
                accessPoints,
                path,
                reactor(acceptors.size()),
                serverIds
            )
        );
    }

    private ReactableAccessPoint accessPoint(String host) {
        return ReactableAccessPoint.builder()
            .id("access-point-" + host)
            .environmentId("environment-" + acceptors.size())
            .host(host)
            .target(ReactableAccessPoint.Target.GATEWAY)
            .build();
    }

    private String host(Shape shape, int index) {
        return switch (shape) {
            case NO_HOST -> null;
            case VHOST -> exactHost(index);
            case MIXED -> random.nextInt(5) == 0 ? exactHost(index) : null;
            case WILDCARD_HEAVY -> random.nextBoolean() ? wildcardHost() : exactHost(index);
        };
    }

    private String exactHost(int index) {
        String host = "tenant-" + index + ".acme.com";
        return random.nextInt(10) == 0 ? host.toUpperCase() : host;
    }

    private String wildcardHost() {
        return pick(List.of("*.acme.com", "*.bar.acme.com", "*acme.com", "*.other.com"));
    }

    /**
     * Most APIs get a context path rooted on their own segment, the way real ones are. The rest draw every
     * segment from a shared vocabulary, which is what produces the nested and near-miss prefixes the index
     * has to get right.
     *
     * <p>Without the unique root the vocabulary caps the population at a few thousand distinct paths, and a
     * corpus asked for more silently saturates: two populations of different requested sizes end up
     * identical, and any measurement across them reads as flat for the wrong reason.
     */
    private String path(int index) {
        int depth = 1 + random.nextInt(4);
        StringBuilder path = new StringBuilder();
        if (random.nextInt(10) < 7) {
            path.append("/svc-").append(index);
        }
        for (int i = 0; i < depth; i++) {
            path.append('/').append(pick(SEGMENTS));
        }
        return random.nextBoolean() ? path.toString() : path + "/";
    }

    /**
     * For every declared acceptor, throws requests that land on it, just beside it, and just past it.
     */
    private void recordRequests(String host, String path) {
        String base = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        requests.add(new Request(requestHost(host), base, pick(SERVER_ID_QUERIES)));
        requests.add(new Request(requestHost(host), base + "/", pick(SERVER_ID_QUERIES)));
        requests.add(new Request(requestHost(host), base + "/deeper/still", pick(SERVER_ID_QUERIES)));
        requests.add(new Request(requestHost(host), base + "suffix", pick(SERVER_ID_QUERIES)));
        requests.add(new Request(requestHost(host), base + "//deeper", pick(SERVER_ID_QUERIES)));
        requests.add(new Request(requestHost(host), "/never/declared/" + random.nextInt(1000), pick(SERVER_ID_QUERIES)));
    }

    private String requestHost(String declaredHost) {
        if (declaredHost == null) {
            return pick(List.of("localhost", "localhost:8082", "api.acme.com"));
        }
        String host = declaredHost.startsWith("*") ? "sub" + declaredHost.substring(1) : declaredHost;
        return switch (random.nextInt(4)) {
            case 0 -> host.toUpperCase();
            case 1 -> host.toLowerCase();
            case 2 -> host + ":8082";
            default -> host;
        };
    }

    /**
     * Shapes no gateway should ever emit, and that the index must nonetheless answer exactly like the
     * scan. A null path is left out: the scan throws on it, so there is nothing to agree with.
     */
    private void addDegenerateRequests() {
        for (String path : List.of("", "/", "//", "/a//b", "no-leading-slash", "store/v1", "/store/", "/STORE")) {
            for (String host : List.of("localhost", "api.acme.com", "API.ACME.COM", "api.acme.com:8082")) {
                requests.add(new Request(host, path, pick(SERVER_ID_QUERIES)));
            }
            requests.add(new Request(null, path, pick(SERVER_ID_QUERIES)));
        }
    }

    private <T> T pick(List<T> candidates) {
        return candidates.get(random.nextInt(candidates.size()));
    }

    private static String key(String host, String path) {
        String normalized = path.endsWith("/") ? path : path + "/";
        return (host == null ? "" : host.toLowerCase()) + "|" + normalized;
    }

    private static ReactorHandler reactor(int index) {
        return new NamedReactorHandler("reactor-" + index);
    }

    /**
     * A real instance rather than a mock: the corpus builds thousands of these, and only its identity
     * matters to the assertions.
     */
    record NamedReactorHandler(String name) implements ReactorHandler {
        @Override
        public List<Acceptor<?>> acceptors() {
            return List.of();
        }

        @Override
        public void handle(ExecutionContext context, Handler<ExecutionContext> endHandler) {
            throw new UnsupportedOperationException("the corpus never executes a request");
        }

        @Override
        public Lifecycle.State lifecycleState() {
            return Lifecycle.State.STARTED;
        }

        @Override
        public ReactorHandler start() {
            return this;
        }

        @Override
        public ReactorHandler stop() {
            return this;
        }
    }
}
