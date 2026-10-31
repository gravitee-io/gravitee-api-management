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
package io.gravitee.gamma.rest.core.observability.catalog.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.DiscoveredAdditionalMetric;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricDiscoveryPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * The behaviour every {@link AdditionalMetricDiscoveryPort} implementation must honour: keys typed by the platform
 * grammar, keys outside it dropped before the cap, the cap applied to the keys listed nowhere else.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class AdditionalMetricDiscoveryPortContractTest {

    protected static final Instant REFRESHED_AT = Instant.parse("2026-10-30T09:00:00Z");
    private static final Instant SEEN = Instant.parse("2026-10-30T08:00:00Z");

    /** A key the pass found, with the time it was last seen. */
    protected record Seen(String rawKey, Instant lastSeen) {}

    /** When the pass of {@link #portServing} ran. */
    protected Instant refreshedAt() {
        return REFRESHED_AT;
    }

    /** A port whose pass, run at {@link #refreshedAt()}, found these keys. */
    protected abstract AdditionalMetricDiscoveryPort portServing(List<Seen> found, boolean truncated);

    @Test
    void should_type_each_key_with_the_platform_grammar() {
        var discovery = portServing(List.of(seen("long_failover_count", 0), seen("keyword_mcp-proxy_tools/call", 1)), false).discover(
            "org",
            "env",
            Set.of()
        );

        assertThat(discovery.fields())
            .extracting(DiscoveredAdditionalMetric::rawKey, DiscoveredAdditionalMetric::valueType)
            .containsExactly(
                tuple("long_failover_count", AdditionalMetricValueType.LONG),
                tuple("keyword_mcp-proxy_tools/call", AdditionalMetricValueType.KEYWORD)
            );
        assertThat(discovery.refreshedAt()).isEqualTo(refreshedAt());
    }

    @Test
    void should_drop_a_key_outside_the_grammar_before_the_cap_so_it_takes_no_one_s_place() {
        List<Seen> found = new ArrayList<>();
        found.add(seen("long_bad key", 0));
        IntStream.range(1, 201).forEach(i -> found.add(seen("long_key-%03d".formatted(i), i)));

        var discovery = portServing(found, false).discover("org", "env", Set.of());

        assertThat(discovery.fields())
            .hasSize(200)
            .noneMatch(field -> field.rawKey().equals("long_bad key"));
        assertThat(discovery.truncated()).isFalse();
    }

    @Test
    void should_cap_the_keys_listed_nowhere_else_and_keep_the_others() {
        List<Seen> found = new ArrayList<>();
        IntStream.range(0, 201).forEach(i -> found.add(seen("long_key-%03d".formatted(i), i)));
        found.add(seen("keyword_mcp-proxy_tools/call", 300));

        var discovery = portServing(found, false).discover("org", "env", Set.of("keyword_mcp-proxy_tools/call"));

        assertThat(discovery.fields())
            .hasSize(201)
            .extracting(DiscoveredAdditionalMetric::rawKey)
            .contains("keyword_mcp-proxy_tools/call")
            .doesNotContain("long_key-200");
        assertThat(discovery.truncated()).isTrue();
    }

    @Test
    void should_keep_a_pass_flagged_truncated_truncated() {
        var discovery = portServing(List.of(seen("long_failover_count", 0)), true).discover("org", "env", Set.of());

        assertThat(discovery.truncated()).isTrue();
    }

    private static Seen seen(String rawKey, int secondsBefore) {
        return new Seen(rawKey, SEEN.minusSeconds(secondsBefore));
    }
}
