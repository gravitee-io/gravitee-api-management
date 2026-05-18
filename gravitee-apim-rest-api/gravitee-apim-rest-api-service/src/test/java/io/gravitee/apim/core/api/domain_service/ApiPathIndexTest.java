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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
    private static final Instant T0 = Instant.parse("2026-05-18T10:00:00Z");

    @Test
    void cold_findConflicts_seeds_via_supplied_seeder_once() {
        var calls = new AtomicInteger();
        Supplier<Stream<Api>> seeder = () -> {
            calls.incrementAndGet();
            return Stream.empty();
        };
        var index = new ApiPathIndex();

        var result = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo").build()), seeder);

        assertThat(result.errors()).isEmpty();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void empty_seed_initializes_watermark_to_EPOCH() {
        var index = new ApiPathIndex();
        var result = index.findConflicts(ENV, API_ID, List.of(), Stream::empty);

        assertThat(result.refreshedAt()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void seed_initializes_watermark_to_max_updated_at_over_rows() {
        var older = apiWithPaths("api-older", "/foo", T0);
        var newer = apiWithPaths("api-newer", "/bar", T0.plusSeconds(60));
        var index = new ApiPathIndex();

        var result = index.findConflicts(ENV, API_ID, List.of(), () -> Stream.of(older, newer));

        assertThat(result.refreshedAt()).isEqualTo(T0.plusSeconds(60));
    }

    @Test
    void index_bumps_watermark() {
        var index = new ApiPathIndex();
        index.findConflicts(ENV, "seed-api", List.of(), Stream::empty);

        index.index(ENV, "new-api", List.of(Path.builder().path("/foo").build()), T0);

        var result = index.findConflicts(ENV, API_ID, List.of(), Stream::empty);
        assertThat(result.refreshedAt()).isEqualTo(T0);
    }

    @Test
    void index_does_not_lower_watermark() {
        var index = new ApiPathIndex();
        index.findConflicts(ENV, "seed-api", List.of(), Stream::empty);
        index.index(ENV, "new-api", List.of(Path.builder().path("/foo").build()), T0.plusSeconds(60));

        // Older update arriving later does not lower the watermark
        index.index(ENV, "other-api", List.of(Path.builder().path("/bar").build()), T0);

        var result = index.findConflicts(ENV, API_ID, List.of(), Stream::empty);
        assertThat(result.refreshedAt()).isEqualTo(T0.plusSeconds(60));
    }

    @Test
    void remove_bumps_watermark() {
        var index = new ApiPathIndex();
        index.findConflicts(ENV, "seed-api", List.of(), Stream::empty);
        index.index(ENV, "victim", List.of(Path.builder().path("/foo").build()), T0);

        index.remove(ENV, "victim", T0.plusSeconds(30));

        var result = index.findConflicts(ENV, API_ID, List.of(), Stream::empty);
        assertThat(result.refreshedAt()).isEqualTo(T0.plusSeconds(30));
    }

    @Test
    void removeForApi_does_not_bump_watermark() {
        var index = new ApiPathIndex();
        index.findConflicts(ENV, "seed-api", List.of(), Stream::empty);
        index.index(ENV, "victim", List.of(Path.builder().path("/foo").build()), T0);

        index.removeForApi("victim");

        var result = index.findConflicts(ENV, API_ID, List.of(), Stream::empty);
        assertThat(result.refreshedAt()).isEqualTo(T0);
    }

    @Test
    void findConflicts_ignores_the_api_being_updated() {
        var self = apiWithPaths(API_ID, "/foo", T0);
        Supplier<Stream<Api>> seeder = () -> Stream.of(self);
        var index = new ApiPathIndex();

        var result = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo/bar").build()), seeder);

        assertThat(result.errors()).isEmpty();
    }

    @Test
    void findConflicts_reports_collision_against_seeded_api() {
        var existing = apiWithPaths("other-api", "/foo", T0);
        Supplier<Stream<Api>> seeder = () -> Stream.of(existing);
        var index = new ApiPathIndex();

        var result = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo/bar").build()), seeder);

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).getMessage()).isEqualTo("Path [/foo/] already exists");
        assertThat(result.conflicts().get(0).apiId()).isEqualTo("other-api");
    }

    @Test
    void index_defensive_copies_paths_so_caller_mutation_does_not_corrupt_snapshot() {
        var index = new ApiPathIndex();
        index.findConflicts(ENV, "seed-api", List.of(), Stream::empty);
        var mutable = new java.util.ArrayList<Path>();
        mutable.add(Path.builder().path("/safe/").build());

        index.index(ENV, "victim", mutable, T0);
        mutable.clear();

        var result = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/safe/bar").build()), Stream::empty);
        assertThat(result.errors()).hasSize(1);
    }

    @Test
    void removeForApi_evicts_entry_from_every_env_snapshot() {
        var index = new ApiPathIndex();
        index.findConflicts("env-A", "seed", List.of(), Stream::empty);
        index.findConflicts("env-B", "seed", List.of(), Stream::empty);
        index.index("env-A", "shared-api", List.of(Path.builder().path("/foo/").build()), T0);
        index.index("env-B", "shared-api", List.of(Path.builder().path("/bar/").build()), T0);

        index.removeForApi("shared-api");

        assertThat(index.findConflicts("env-A", "x", List.of(Path.builder().path("/foo").build()), Stream::empty).errors()).isEmpty();
        assertThat(index.findConflicts("env-B", "x", List.of(Path.builder().path("/bar").build()), Stream::empty).errors()).isEmpty();
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
        index.index(ENV, "dropped-api", List.of(Path.builder().path("/foo/").build()), T0);

        var result = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo/bar").build()), Stream::empty);

        assertThat(result.errors()).isEmpty();
    }

    @Test
    void index_on_ready_env_makes_paths_visible_to_next_findConflicts() {
        var index = new ApiPathIndex();
        index.findConflicts(ENV, "seed-api", List.of(), Stream::empty);

        index.index(ENV, "new-api", List.of(Path.builder().path("/foo/").build()), T0);

        var result = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo/bar").build()), Stream::empty);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).getMessage()).isEqualTo("Path [/foo/] already exists");
    }

    @Test
    void findConflicts_isolates_paths_by_host_bucket() {
        var existingHost = apiWithHostPath("other-api", "h1", "/foo", T0);
        var index = new ApiPathIndex();

        var sameHostResult = index.findConflicts(ENV, API_ID, List.of(Path.builder().host("h1").path("/foo/bar").build()), () ->
            Stream.of(existingHost)
        );
        var otherHostResult = index.findConflicts(ENV, API_ID, List.of(Path.builder().host("h2").path("/foo/bar").build()), Stream::empty);

        assertThat(sameHostResult.errors()).hasSize(1);
        assertThat(otherHostResult.errors()).isEmpty();
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

        var result = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo").build()), seeder);
        assertThat(result.errors()).isEmpty();
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void remove_drops_paths_from_ready_env_snapshot() {
        var index = new ApiPathIndex();
        index.findConflicts(ENV, "seed-api", List.of(), Stream::empty);
        index.index(ENV, "to-be-removed", List.of(Path.builder().path("/foo/").build()), T0);

        index.remove(ENV, "to-be-removed", T0.plusSeconds(1));

        var result = index.findConflicts(ENV, API_ID, List.of(Path.builder().path("/foo/bar").build()), Stream::empty);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void findConflicts_isolates_envs() {
        var env1Api = apiWithPaths("other-api", "/foo", T0);
        var index = new ApiPathIndex();
        index.findConflicts("env-1", API_ID, List.of(), () -> Stream.of(env1Api));

        var result = index.findConflicts("env-2", API_ID, List.of(Path.builder().path("/foo/bar").build()), Stream::empty);

        assertThat(result.errors()).isEmpty();
    }

    @Test
    void snapshotOf_returns_immutable_view_with_watermark() {
        var seeded = apiWithPaths("seeded", "/foo", T0);
        var index = new ApiPathIndex();
        var snapshot = index.snapshotOf(ENV, () -> Stream.of(seeded));

        assertThat(snapshot.pathsByApiId()).containsOnlyKeys("seeded");
        assertThat(snapshot.refreshedAt()).isEqualTo(T0);
    }

    @Test
    void scanPaths_static_helper_finds_conflicts_against_arbitrary_view() {
        var paths = java.util.Map.of("foreign", List.of(Path.builder().path("/foo/").build()));

        var conflicts = ApiPathIndex.scanPaths(paths, API_ID, List.of(Path.builder().path("/foo/bar").build()));

        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).apiId()).isEqualTo("foreign");
    }

    private static Api apiWithHostPath(String apiId, String host, String path, Instant updatedAt) {
        return ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id(apiId)
            .updatedAt(ZonedDateTime.ofInstant(updatedAt, ZoneOffset.UTC))
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

    private static Api apiWithPaths(String apiId, String path, Instant updatedAt) {
        return ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id(apiId)
            .updatedAt(ZonedDateTime.ofInstant(updatedAt, ZoneOffset.UTC))
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
