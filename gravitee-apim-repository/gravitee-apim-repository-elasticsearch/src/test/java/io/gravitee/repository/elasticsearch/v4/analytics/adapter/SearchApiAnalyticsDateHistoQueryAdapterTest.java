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
package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsDateHistoQuery;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsField;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchApiAnalyticsDateHistoQueryAdapterTest {

    @Test
    void should_build_date_histo_query_with_min_doc_count_and_extended_bounds() {
        var now = Instant.parse("2021-01-01T00:00:00Z");
        var from = now.truncatedTo(ChronoUnit.DAYS);
        var to = from.plus(Duration.ofDays(1));

        var query = new ApiAnalyticsDateHistoQuery("api-id", from, to, ApiAnalyticsField.STATUS, Duration.ofMinutes(30));

        var result = SearchApiAnalyticsDateHistoQueryAdapter.adapt(query);

        assertThatJson(result).node("aggs.by_date.date_histogram.min_doc_count").isEqualTo(0);
        assertThatJson(result).node("aggs.by_date.date_histogram.extended_bounds.min").isEqualTo(1609459200000L);
        assertThatJson(result).node("aggs.by_date.date_histogram.extended_bounds.max").isEqualTo(1609545600000L);
        assertThatJson(result).node("aggs.by_date.date_histogram.fixed_interval").isEqualTo("1800000ms");
        assertThatJson(result).node("aggs.by_date.aggs.by_field.terms.field").isEqualTo("status");
    }
}
