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
package io.gravitee.gamma.rest.resources.observability.logs.dto;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.resources.observability.dashboards.dto.SaveDashboardFilterDto;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/** Operators are read case-insensitively, whatever the server's default locale: in Turkish, "in" upper-cases to "İN". */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FilterConditionDtoTest {

    private Locale defaultLocale;

    @BeforeEach
    void useTurkishLocale() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    void should_read_a_lowercase_operator_of_a_search_filter_under_any_locale() {
        var condition = new FilterConditionDto("API", "in", List.of("api-1")).toCore();

        assertThat(condition.operator()).isEqualTo(FilterOperator.IN);
    }

    @Test
    void should_read_a_lowercase_operator_of_a_dashboard_filter_under_any_locale() {
        var filter = new SaveDashboardFilterDto("API", null, "in", List.of("api-1"), true).toCore();

        assertThat(filter.condition().operator()).isEqualTo(FilterOperator.IN);
    }
}
