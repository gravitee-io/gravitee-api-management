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
package io.gravitee.apim.infra.adapter;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaPortRangeAdapterTest {

    @Test
    void should_round_trip_core_to_repository_and_back() {
        var coreNow = ZonedDateTime.of(2026, 4, 23, 12, 0, 0, 0, ZoneOffset.UTC);
        var source = io.gravitee.apim.core.plan.model.KafkaPortRange.builder()
            .planId("plan-1")
            .apiId("api-1")
            .environmentId("env-1")
            .bootstrapPort(9092)
            .rangeStart(9100)
            .rangeEnd(9102)
            .createdAt(coreNow)
            .updatedAt(coreNow)
            .build();

        var repository = KafkaPortRangeAdapter.INSTANCE.toRepository(source);
        var roundTripped = KafkaPortRangeAdapter.INSTANCE.fromRepository(repository);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(roundTripped.getPlanId()).isEqualTo(source.getPlanId());
            soft.assertThat(roundTripped.getApiId()).isEqualTo(source.getApiId());
            soft.assertThat(roundTripped.getEnvironmentId()).isEqualTo(source.getEnvironmentId());
            soft.assertThat(roundTripped.getBootstrapPort()).isEqualTo(source.getBootstrapPort());
            soft.assertThat(roundTripped.getRangeStart()).isEqualTo(source.getRangeStart());
            soft.assertThat(roundTripped.getRangeEnd()).isEqualTo(source.getRangeEnd());
            soft.assertThat(roundTripped.getCreatedAt()).isEqualTo(source.getCreatedAt());
            soft.assertThat(roundTripped.getUpdatedAt()).isEqualTo(source.getUpdatedAt());
        });
    }

    @Test
    void should_map_instant_to_utc_zoned_date_time() {
        var repo = io.gravitee.repository.management.model.KafkaPortRange.builder()
            .planId("plan-1")
            .apiId("api-1")
            .environmentId("env-1")
            .bootstrapPort(9092)
            .rangeStart(9100)
            .rangeEnd(9102)
            .createdAt(Instant.parse("2026-04-23T12:00:00Z"))
            .updatedAt(Instant.parse("2026-04-23T12:00:00Z"))
            .build();

        var core = KafkaPortRangeAdapter.INSTANCE.fromRepository(repo);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(core.getCreatedAt().getZone()).isEqualTo(ZoneOffset.UTC);
            soft.assertThat(core.getCreatedAt().toInstant()).isEqualTo(Instant.parse("2026-04-23T12:00:00Z"));
        });
    }
}
