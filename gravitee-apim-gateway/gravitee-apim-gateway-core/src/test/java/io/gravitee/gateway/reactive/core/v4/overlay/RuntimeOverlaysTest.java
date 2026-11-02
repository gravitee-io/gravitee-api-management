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
package io.gravitee.gateway.reactive.core.v4.overlay;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.opentelemetry.sampling.SamplingConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RuntimeOverlaysTest {

    private static final String API_ID = "api-id";

    private final AtomicLong now = new AtomicLong(1_000);
    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
    private final List<String> released = new ArrayList<>();
    private final RuntimeOverlays runtimeOverlays = new RuntimeOverlays(scheduler, now::get);

    {
        runtimeOverlays.registerContributor(
            new RuntimeOverlayContributor() {
                @Override
                public String kind() {
                    return "test";
                }

                @Override
                public RuntimeOverlayContribution contribute(final RuntimeOverlay runtimeOverlay) {
                    return RuntimeOverlayContribution.ofInternalAttributes(Map.of("attr", runtimeOverlay.id()));
                }

                @Override
                public void release(final RuntimeOverlay runtimeOverlay) {
                    released.add(runtimeOverlay.id());
                }
            }
        );
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdownNow();
    }

    @Test
    void should_expose_the_contribution_of_an_active_runtime_overlay() {
        // Given
        runtimeOverlays.register(overlay("o1", RuntimeOverlayStatus.ACTIVE, 61_000));

        // When
        var actives = runtimeOverlays.activeFor(API_ID);

        // Then
        assertThat(actives)
            .singleElement()
            .satisfies(active -> assertThat(active.contribution().internalAttributes()).containsEntry("attr", "o1"));
        assertThat(runtimeOverlays.activeFor("other-api")).isEmpty();
    }

    @Test
    void should_hide_a_runtime_overlay_whose_end_is_past_before_its_timer_fires() {
        // Given
        runtimeOverlays.register(overlay("o1", RuntimeOverlayStatus.ACTIVE, 61_000));

        // When
        now.set(61_000);

        // Then
        assertThat(runtimeOverlays.activeFor(API_ID)).isEmpty();
    }

    @Test
    void should_remove_and_release_a_runtime_overlay_when_its_timer_fires() throws InterruptedException {
        // Given
        runtimeOverlays.register(overlay("o1", RuntimeOverlayStatus.ACTIVE, 1_050));

        // When
        scheduler.shutdown();
        scheduler.awaitTermination(5, TimeUnit.SECONDS);

        // Then
        assertThat(released).containsExactly("o1");
        now.set(0);
        assertThat(runtimeOverlays.activeFor(API_ID)).isEmpty();
    }

    @Test
    void should_remove_and_release_a_stopped_runtime_overlay() {
        // Given
        runtimeOverlays.register(overlay("o1", RuntimeOverlayStatus.ACTIVE, 61_000));

        // When
        runtimeOverlays.register(overlay("o1", RuntimeOverlayStatus.STOPPED, 0));

        // Then
        assertThat(runtimeOverlays.activeFor(API_ID)).isEmpty();
        assertThat(released).containsExactly("o1");
        assertThat(scheduler.getQueue()).allMatch(task -> ((Future<?>) task).isCancelled());
    }

    @Test
    void should_ignore_the_same_event_read_twice() {
        // Given
        runtimeOverlays.register(overlay("o1", RuntimeOverlayStatus.ACTIVE, 61_000));
        var first = runtimeOverlays.activeFor(API_ID).getFirst();

        // When
        runtimeOverlays.register(overlay("o1", RuntimeOverlayStatus.ACTIVE, 61_000));

        // Then
        assertThat(runtimeOverlays.activeFor(API_ID)).containsExactly(first);
    }

    @Test
    void should_ignore_a_runtime_overlay_already_ended_or_without_contributor() {
        // When
        runtimeOverlays.register(overlay("o1", RuntimeOverlayStatus.ACTIVE, 500));
        runtimeOverlays.register(new RuntimeOverlay("o2", "unknown", API_ID, 0, 61_000, RuntimeOverlayStatus.ACTIVE, Map.of()));

        // Then
        assertThat(runtimeOverlays.activeFor(API_ID)).isEmpty();
    }

    @Test
    void should_give_the_sampling_of_the_first_runtime_overlay_that_sets_one() {
        // Given
        var sampling = new SamplingConfig(0.5, false, Set.of("POST"), List.of(), Set.of(405), true);
        runtimeOverlays.registerContributor(
            new RuntimeOverlayContributor() {
                @Override
                public String kind() {
                    return "sampling";
                }

                @Override
                public RuntimeOverlayContribution contribute(final RuntimeOverlay runtimeOverlay) {
                    return RuntimeOverlayContribution.ofSampling(sampling);
                }
            }
        );
        runtimeOverlays.register(overlay("o1", RuntimeOverlayStatus.ACTIVE, 61_000));
        runtimeOverlays.register(new RuntimeOverlay("o2", "sampling", API_ID, 0, 61_000, RuntimeOverlayStatus.ACTIVE, Map.of()));

        // Then
        assertThat(runtimeOverlays.samplingFor(API_ID)).contains(sampling);
        assertThat(runtimeOverlays.samplingFor("other-api")).isEmpty();
    }

    private static RuntimeOverlay overlay(final String id, final RuntimeOverlayStatus status, final long endAt) {
        return new RuntimeOverlay(id, "test", API_ID, 0, endAt, status, Map.of());
    }
}
