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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import fixtures.AnalyticsFixtures;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.sampling.SamplingType;
import io.gravitee.rest.api.management.v2.rest.model.Sampling;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class AnalyticsMapperTest {

    private final AnalyticsMapper analyticsMapper = Mappers.getMapper(AnalyticsMapper.class);

    @Test
    void shouldMapDefinitionModelToRestModel() {
        Analytics analytics = AnalyticsFixtures.anAnalytics();
        var mappedAnalytics = analyticsMapper.toAnalytics(analytics);
        assertThat(mappedAnalytics).isNotNull();
        assertThat(mappedAnalytics.getEnabled()).isEqualTo(analytics.isEnabled());
        assertThat(mappedAnalytics.getSampling()).isNotNull();
        assertThat("10").isEqualTo(mappedAnalytics.getSampling().getValue());
        assertThat(Sampling.TypeEnum.COUNT).isEqualTo(mappedAnalytics.getSampling().getType());
        assertThat(mappedAnalytics.getLogging()).isNotNull();
    }

    @Test
    void shouldMapRestModelToDefinitionModel() {
        io.gravitee.rest.api.management.v2.rest.model.Analytics analytics = AnalyticsFixtures.aBasicAnalytics();
        var mappedAnalytics = analyticsMapper.fromAnalytics(analytics);
        assertThat(mappedAnalytics).isNotNull();
        assertThat(mappedAnalytics.isEnabled()).isEqualTo(analytics.getEnabled());
        assertThat(mappedAnalytics.getMessageSampling()).isNotNull();
        assertThat("10").isEqualTo(mappedAnalytics.getMessageSampling().getValue());
        assertThat(Sampling.TypeEnum.COUNT.toString()).isEqualTo(mappedAnalytics.getMessageSampling().getType().toString());
        assertThat(mappedAnalytics.getLogging()).isNotNull();
    }
}
