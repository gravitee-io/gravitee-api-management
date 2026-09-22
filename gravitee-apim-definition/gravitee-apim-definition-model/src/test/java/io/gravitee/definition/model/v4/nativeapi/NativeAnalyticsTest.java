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
package io.gravitee.definition.model.v4.nativeapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeAnalyticsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_default_reporter_metrics_enabled_when_absent_in_json() throws Exception {
        var analytics = objectMapper.readValue("{\"enabled\":true}", NativeAnalytics.class);

        assertThat(analytics.isReporterMetricsEnabled()).isTrue();
    }

    @Test
    void should_resolve_an_unconfigured_api_to_what_it_already_reports_today() throws Exception {
        // The whole point of the field being nullable. An API definition written before this setting existed
        // has no connectionEvents, and must keep reporting exactly what it reports today — not the richer set
        // a newly created API gets. Give the field a @Builder.Default and this test is the one that fails.
        var analytics = objectMapper.readValue("{\"enabled\":true}", NativeAnalytics.class);

        assertThat(analytics.getConnectionEvents()).isNull();
        assertThat(analytics.effectiveConnectionEvents()).isEqualTo(NativeConnectionEvent.LEGACY_DEFAULTS);
        assertThat(analytics.effectiveConnectionEvents()).doesNotContain(NativeConnectionEvent.DISCONNECTED);
    }

    @Test
    void should_not_default_connection_events_through_the_builder_either() {
        // The no-args constructor and the builder must agree: Lombok applies @Builder.Default in both, so a
        // default added there would reach deserialized definitions too.
        assertThat(NativeAnalytics.builder().build().getConnectionEvents()).isNull();
        assertThat(new NativeAnalytics().getConnectionEvents()).isNull();
    }

    @Test
    void should_honour_an_explicit_empty_selection_rather_than_falling_back() {
        // Unchecking everything is a decision, not an absence of one.
        var analytics = NativeAnalytics.builder().connectionEvents(Set.of()).build();

        assertThat(analytics.effectiveConnectionEvents()).isEmpty();
    }

    @Test
    void should_preserve_connection_events_through_json() throws Exception {
        var original = NativeAnalytics.builder()
            .connectionEvents(Set.of(NativeConnectionEvent.CONNECTED, NativeConnectionEvent.DISCONNECTED))
            .build();

        var deserialized = objectMapper.readValue(objectMapper.writeValueAsString(original), NativeAnalytics.class);

        assertThat(deserialized.getConnectionEvents()).containsExactlyInAnyOrder(
            NativeConnectionEvent.CONNECTED,
            NativeConnectionEvent.DISCONNECTED
        );
    }

    @Test
    void should_not_serialize_the_resolved_set_into_the_definition() throws Exception {
        // effectiveConnectionEvents() deliberately has no 'get' prefix: persisting a derived value would
        // outlive the rule that produced it, and an API would be stuck with whatever the legacy default was
        // on the day it was written.
        var json = objectMapper.writeValueAsString(NativeAnalytics.builder().build());

        assertThat(json).doesNotContain("effectiveConnectionEvents");
    }

    @Test
    void should_preserve_reporter_metrics_enabled_through_json() throws Exception {
        var original = NativeAnalytics.builder().enabled(true).reporterMetricsEnabled(false).build();

        var deserialized = objectMapper.readValue(objectMapper.writeValueAsString(original), NativeAnalytics.class);

        assertThat(deserialized).isEqualTo(original);
    }
}
