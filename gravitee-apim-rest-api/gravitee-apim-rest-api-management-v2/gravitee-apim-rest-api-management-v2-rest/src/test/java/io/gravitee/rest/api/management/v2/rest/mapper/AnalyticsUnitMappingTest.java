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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.UnitName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * {@code UnitName} is generated from the OpenAPI enum and resolved by name at runtime: a core unit the OpenAPI does not
 * list would throw on the first response carrying it. This pins that the OpenAPI lists every unit the engine reports.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsUnitMappingTest {

    @ParameterizedTest
    @EnumSource(MetricSpec.Unit.class)
    void should_document_every_unit_the_engine_reports(MetricSpec.Unit unit) {
        assertThat(AnalyticsMeasuresMapper.INSTANCE.map(unit)).isEqualTo(UnitName.valueOf(unit.name()));
    }
}
