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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeFacetsUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeTimeSeriesUseCase;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityAnalyticsDataPortAdapterTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-1";

    @Mock
    private ComputeMeasuresUseCase computeMeasuresUseCase;

    @Mock
    private ComputeFacetsUseCase computeFacetsUseCase;

    @Mock
    private ComputeTimeSeriesUseCase computeTimeSeriesUseCase;

    @Mock
    private UserContextLoader userContextLoader;

    private ObservabilityAnalyticsDataPortAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ObservabilityAnalyticsDataPortAdapter(
            computeMeasuresUseCase,
            computeFacetsUseCase,
            computeTimeSeriesUseCase,
            userContextLoader,
            new ObjectMapper()
        );
    }

    @Test
    void should_map_an_authz_api_to_the_authz_gamma_api_type() {
        when(userContextLoader.loadApis(any())).thenAnswer(invocation ->
            ((UserContext) invocation.getArgument(0)).withApis(
                List.of(Api.builder().id("api-1").name("Authz API").type(io.gravitee.definition.model.v4.ApiType.AUTHZ).build())
            )
        );

        var accessibleApis = adapter.loadAccessibleApis(ORG, ENV);

        assertThat(accessibleApis).hasSize(1);
        assertThat(accessibleApis.getFirst().type()).isEqualTo(ApiType.AUTHZ);
    }
}
