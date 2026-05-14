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
package io.gravitee.apim.core.api.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ApiPathIndexTest {

    private static final String ENV = "env-1";
    private static final String API_ID = "api-1";

    @Test
    void cold_findConflicts_seeds_via_supplied_seeder_once() {
        var calls = new AtomicInteger();
        Supplier<Stream<Api>> seeder = () -> {
            calls.incrementAndGet();
            return Stream.empty();
        };
        var index = new ApiPathIndex();

        var errors = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo").build()), seeder);

        assertThat(errors).isEmpty();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void findConflicts_ignores_the_api_being_updated() {
        var self = apiWithPaths(API_ID, "/foo");
        Supplier<Stream<Api>> seeder = () -> Stream.of(self);
        var index = new ApiPathIndex();

        var errors = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo/bar").build()), seeder);

        assertThat(errors).isEmpty();
    }

    @Test
    void findConflicts_reports_collision_against_seeded_api() {
        var existing = apiWithPaths("other-api", "/foo");
        Supplier<Stream<Api>> seeder = () -> Stream.of(existing);
        var index = new ApiPathIndex();

        var errors = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo/bar").build()), seeder);

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getMessage()).isEqualTo("Path [/foo/] already exists");
    }

    @Test
    void index_defensive_copies_paths_so_caller_mutation_does_not_corrupt_snapshot() {
        var index = new ApiPathIndex();
        index.findConflicts(ENV, "seed-api", List.of(), Stream::empty);
        var mutable = new java.util.ArrayList<Path>();
        mutable.add(Path.builder().path("/safe/").build());

        index.index(ENV, "victim", mutable);
        mutable.clear();

        var errors = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/safe/bar").build()), Stream::empty);
        assertThat(errors).hasSize(1);
    }

    @Test
    void removeForApi_evicts_entry_from_every_env_snapshot() {
        var index = new ApiPathIndex();
        // seed two distinct envs (READY)
        index.findConflicts("env-A", "seed", List.of(), Stream::empty);
        index.findConflicts("env-B", "seed", List.of(), Stream::empty);
        // place the same apiId in both
        index.index("env-A", "shared-api", List.of(Path.builder().path("/foo/").build()));
        index.index("env-B", "shared-api", List.of(Path.builder().path("/bar/").build()));

        index.removeForApi("shared-api");

        assertThat(index.findConflicts("env-A", "x", List.of(Path.builder().path("/foo").build()), Stream::empty)).isEmpty();
        assertThat(index.findConflicts("env-B", "x", List.of(Path.builder().path("/bar").build()), Stream::empty)).isEmpty();
    }

    @Test
    void invalidate_evicts_env_snapshot_so_next_read_reseeds() {
        var calls = new AtomicInteger();
        Supplier<Stream<Api>> seeder = () -> {
            calls.incrementAndGet();
            return Stream.empty();
        };
        var index = new ApiPathIndex();
        index.findConflicts(ENV, "x", List.of(), seeder);
        assertThat(calls.get()).isEqualTo(1);

        index.invalidate(ENV);
        index.findConflicts(ENV, "x", List.of(), seeder);

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void index_on_unseeded_env_is_dropped() {
        var index = new ApiPathIndex();
        index.index(ENV, "dropped-api", List.of(Path.builder().path("/foo/").build()));

        var errors = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo/bar").build()), Stream::empty);

        assertThat(errors).isEmpty();
    }

    @Test
    void index_on_ready_env_makes_paths_visible_to_next_findConflicts() {
        var index = new ApiPathIndex();
        index.findConflicts(ENV, "seed-api", List.of(), Stream::empty);

        index.index(ENV, "new-api", List.of(Path.builder().path("/foo/").build()));

        var errors = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo/bar").build()), Stream::empty);
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getMessage()).isEqualTo("Path [/foo/] already exists");
    }

    @Test
    void findConflicts_isolates_paths_by_host_bucket() {
        var existingHost = apiWithHostPath("other-api", "h1", "/foo");
        var index = new ApiPathIndex();

        var sameHostErrors = index.findConflicts(ENV, API_ID, List.of(Path.builder().host("h1").path("/foo/bar").build()), () ->
            Stream.of(existingHost)
        );
        var otherHostErrors = index.findConflicts(ENV, API_ID, List.of(Path.builder().host("h2").path("/foo/bar").build()), Stream::empty);

        assertThat(sameHostErrors).hasSize(1);
        assertThat(otherHostErrors).isEmpty();
    }

    @Test
    void concurrent_readers_share_a_single_seed() throws Exception {
        var seedInvocations = new AtomicInteger();
        var startedSeeding = new CountDownLatch(1);
        var releaseSeed = new CountDownLatch(1);
        Supplier<Stream<Api>> seeder = () -> {
            seedInvocations.incrementAndGet();
            startedSeeding.countDown();
            try {
                releaseSeed.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Stream.empty();
        };
        var index = new ApiPathIndex();
        var executor = Executors.newFixedThreadPool(8);
        try {
            var futures = IntStream.range(0, 8)
                .mapToObj(i -> executor.submit(() -> index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/x").build()), seeder)))
                .toList();
            assertThat(startedSeeding.await(5, TimeUnit.SECONDS)).isTrue();
            releaseSeed.countDown();
            for (var future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(seedInvocations.get()).isEqualTo(1);
    }

    @Test
    void seed_failure_propagates_and_next_read_retries() {
        var attempts = new AtomicInteger();
        Supplier<Stream<Api>> seeder = () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("DB down");
            }
            return Stream.empty();
        };
        var index = new ApiPathIndex();

        assertThatThrownBy(() -> index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo").build()), seeder)).isInstanceOf(
            IllegalStateException.class
        );

        var errors = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo").build()), seeder);
        assertThat(errors).isEmpty();
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void remove_drops_paths_from_ready_env_snapshot() {
        var index = new ApiPathIndex();
        index.findConflicts(ENV, "seed-api", List.of(), Stream::empty);
        index.index(ENV, "to-be-removed", List.of(Path.builder().path("/foo/").build()));

        index.remove(ENV, "to-be-removed");

        var errors = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo/bar").build()), Stream::empty);
        assertThat(errors).isEmpty();
    }

    @Test
    void findConflicts_isolates_envs() {
        var env1Api = apiWithPaths("other-api", "/foo");
        var index = new ApiPathIndex();
        index.findConflicts("env-1", API_ID, List.of(), () -> Stream.of(env1Api));

        var errors = index.findConflicts("env-2", API_ID, List.of(Path.builder().path("/foo/bar").build()), Stream::empty);

        assertThat(errors).isEmpty();
    }

    private static Api apiWithHostPath(String apiId, String host, String path) {
        return ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id(apiId)
            .apiDefinitionValue(
                io.gravitee.definition.model.v4.Api.builder()
                    .id(apiId)
                    .definitionVersion(DefinitionVersion.V4)
                    .type(ApiType.PROXY)
                    .listeners(
                        List.of(
                            HttpListener.builder()
                                .paths(List.of(io.gravitee.definition.model.v4.listener.http.Path.builder().host(host).path(path).build()))
                                .entrypoints(List.of(Entrypoint.builder().type("http-proxy").configuration("{}").build()))
                                .build()
                        )
                    )
                    .tags(Set.of())
                    .build()
            )
            .build();
    }

    private static Api apiWithPaths(String apiId, String path) {
        return ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id(apiId)
            .apiDefinitionValue(
                io.gravitee.definition.model.v4.Api.builder()
                    .id(apiId)
                    .definitionVersion(DefinitionVersion.V4)
                    .type(ApiType.PROXY)
                    .listeners(
                        List.of(
                            HttpListener.builder()
                                .paths(List.of(io.gravitee.definition.model.v4.listener.http.Path.builder().path(path).build()))
                                .entrypoints(List.of(Entrypoint.builder().type("http-proxy").configuration("{}").build()))
                                .build()
                        )
                    )
                    .tags(Set.of())
                    .build()
            )
            .build();
    }
}
