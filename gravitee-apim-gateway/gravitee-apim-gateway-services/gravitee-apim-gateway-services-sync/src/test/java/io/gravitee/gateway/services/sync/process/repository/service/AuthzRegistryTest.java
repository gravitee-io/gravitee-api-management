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
package io.gravitee.gateway.services.sync.process.repository.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthzRegistryTest {

    private AuthzRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AuthzRegistry(null);
    }

    @Test
    void register_then_isResourceDeployed_returns_true() {
        registry.register(List.of("api.1", "mcp.1.tool-a"));

        assertThat(registry.isResourceDeployed("api.1")).isTrue();
        assertThat(registry.isResourceDeployed("mcp.1.tool-a")).isTrue();
    }

    @Test
    void isResourceDeployed_returns_false_for_unknown_id() {
        assertThat(registry.isResourceDeployed("api.never-registered")).isFalse();
    }

    @Test
    void isResourceDeployed_returns_false_for_null_id_without_throwing() {
        assertThat(registry.isResourceDeployed(null)).isFalse();
    }

    @Test
    void unregister_removes_only_the_listed_ids() {
        registry.register(List.of("api.1", "api.2", "api.3"));

        registry.unregister(List.of("api.2"));

        assertThat(registry.isResourceDeployed("api.1")).isTrue();
        assertThat(registry.isResourceDeployed("api.2")).isFalse();
        assertThat(registry.isResourceDeployed("api.3")).isTrue();
    }

    @Test
    void register_is_idempotent_at_per_id_level() {
        registry.register(List.of("api.1"));
        registry.register(List.of("api.1"));

        assertThat(registry.snapshotForTesting()).containsExactly("api.1");
    }

    @Test
    void unregister_unknown_id_is_a_safe_noop() {
        registry.register(List.of("api.1"));

        registry.unregister(List.of("api.never-registered"));

        assertThat(registry.isResourceDeployed("api.1")).isTrue();
    }

    @Test
    void register_skips_null_and_blank_ids() {
        registry.register(Arrays.asList("api.1", null, "", "  "));

        assertThat(registry.snapshotForTesting()).containsExactly("api.1");
    }

    @Test
    void register_with_null_collection_is_a_safe_noop() {
        registry.register(null);
        registry.unregister(null);

        assertThat(registry.snapshotForTesting()).isEmpty();
    }

    @Test
    void register_with_empty_collection_is_a_safe_noop() {
        registry.register(Collections.emptyList());

        assertThat(registry.snapshotForTesting()).isEmpty();
    }

    @Test
    void concurrent_register_and_isResourceDeployed_do_not_corrupt_state() throws Exception {
        int n = 64;
        ExecutorService writers = Executors.newFixedThreadPool(8);
        ExecutorService readers = Executors.newFixedThreadPool(4);
        AtomicBoolean failed = new AtomicBoolean(false);

        try {
            IntStream.range(0, n).forEach(i ->
                writers.submit(() -> {
                    String id = "api." + i;
                    try {
                        for (int j = 0; j < 100 && !failed.get(); j++) {
                            registry.register(List.of(id));
                            registry.unregister(List.of(id));
                        }
                    } catch (RuntimeException e) {
                        failed.set(true);
                    }
                })
            );
            IntStream.range(0, 4).forEach(i ->
                readers.submit(() -> {
                    for (int j = 0; j < 1000 && !failed.get(); j++) {
                        try {
                            registry.isResourceDeployed("api." + (j % n));
                        } catch (RuntimeException e) {
                            failed.set(true);
                        }
                    }
                })
            );

            writers.shutdown();
            readers.shutdown();
            assertThat(writers.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
            assertThat(readers.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            writers.shutdownNow();
            readers.shutdownNow();
        }

        assertThat(failed.get()).as("a reader or writer threw an exception under contention").isFalse();
    }
}
