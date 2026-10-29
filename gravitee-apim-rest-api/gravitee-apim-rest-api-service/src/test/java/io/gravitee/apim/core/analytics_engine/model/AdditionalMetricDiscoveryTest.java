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
package io.gravitee.apim.core.analytics_engine.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalMetricDiscoveryTest {

    private static final Instant REFRESHED_AT = Instant.parse("2026-10-29T12:00:00Z");

    @Test
    void should_keep_the_200_most_recently_seen_keys_and_say_others_were_left_out() {
        var discovery = new AdditionalMetricDiscovery(seenKeys(201), REFRESHED_AT, false);

        var capped = discovery.capDiscovered(Set.of());

        assertThat(capped.metrics())
            .hasSize(AdditionalMetricDiscovery.MAX_DISCOVERED_ENTRIES)
            .isEqualTo(discovery.metrics().subList(0, 200));
        assertThat(capped.truncated()).isTrue();
        assertThat(capped.refreshedAt()).isEqualTo(REFRESHED_AT);
    }

    @Test
    void should_keep_every_key_up_to_the_cap_without_saying_anything_was_left_out() {
        var discovery = new AdditionalMetricDiscovery(seenKeys(200), REFRESHED_AT, false);

        assertThat(discovery.capDiscovered(Set.of())).isEqualTo(discovery);
    }

    @Test
    void should_never_count_nor_leave_out_a_key_listed_under_another_origin() {
        var metrics = seenKeys(202);
        var builtInSeenLast = metrics.get(201).rawKey();
        var declaredSeenFirst = metrics.get(0).rawKey();
        var discovery = new AdditionalMetricDiscovery(metrics, REFRESHED_AT, false);

        var capped = discovery.capDiscovered(Set.of(builtInSeenLast, declaredSeenFirst));

        assertThat(capped.metrics()).isEqualTo(metrics);
        assertThat(capped.truncated()).isFalse();
    }

    @Test
    void should_keep_the_most_recently_seen_keys_whatever_order_they_come_in() {
        var seenFirst = seenKeys(201);
        var shuffled = new ArrayList<>(seenFirst);
        Collections.reverse(shuffled);
        var tied = new DiscoveredAdditionalMetric("long_a-tie", seenFirst.get(0).lastSeen());
        shuffled.add(tied);

        var capped = new AdditionalMetricDiscovery(shuffled, REFRESHED_AT, false).capDiscovered(Set.of());

        assertThat(capped.metrics()).containsExactlyElementsOf(Stream.concat(Stream.of(tied), seenFirst.subList(0, 199).stream()).toList());
        assertThat(capped.truncated()).isTrue();
    }

    @Test
    void should_stay_truncated_when_the_pass_was() {
        var discovery = new AdditionalMetricDiscovery(seenKeys(3), REFRESHED_AT, true);

        assertThat(discovery.capDiscovered(Set.of()).truncated()).isTrue();
    }

    @Test
    void should_list_nothing_and_say_it_is_incomplete_when_unavailable() {
        assertThat(AdditionalMetricDiscovery.unavailable(REFRESHED_AT)).isEqualTo(
            new AdditionalMetricDiscovery(List.of(), REFRESHED_AT, true)
        );
    }

    /** {@code count} keys, most recently seen first. */
    private static List<DiscoveredAdditionalMetric> seenKeys(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> new DiscoveredAdditionalMetric("long_key-%03d".formatted(i), REFRESHED_AT.minus(Duration.ofMinutes(i))))
            .toList();
    }
}
