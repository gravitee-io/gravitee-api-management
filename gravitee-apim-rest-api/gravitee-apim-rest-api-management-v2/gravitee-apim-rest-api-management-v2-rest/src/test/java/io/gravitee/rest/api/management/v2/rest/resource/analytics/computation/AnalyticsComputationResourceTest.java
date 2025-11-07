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
package io.gravitee.rest.api.management.v2.rest.resource.analytics.computation;

import static assertions.MAPIAssertions.assertThat;
import static fixtures.AnalyticsEngineFixtures.*;

import inmemory.HTTPProxyDataPlaneQueryServiceInMemory;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Measure;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasureName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasuresResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasuresResponseMetricsInner;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricName;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import jakarta.ws.rs.client.Entity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsComputationResourceTest extends ApiResourceTest {

    @Autowired
    HTTPProxyDataPlaneQueryServiceInMemory httpProxyDataPlaneQueryService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/analytics";
    }

    @Nested
    class Measures {

        @BeforeEach
        void setUp() {
            httpProxyDataPlaneQueryService.initWith(
                List.of(
                    new io.gravitee.apim.core.analytics_engine.model.MeasuresResponse(
                        List.of(
                            new MetricMeasuresResponse(
                                MetricSpec.Name.HTTP_REQUESTS,
                                List.of(new io.gravitee.apim.core.analytics_engine.model.Measure(MetricSpec.Measure.COUNT, 42))
                            )
                        )
                    )
                )
            );
        }

        @Test
        void should_return_request_count() {
            var response = rootTarget().path("measures").request().post(Entity.json(aRequestCountMeasureRequest()));

            assertThat(response)
                .hasStatus(200)
                .asEntity(MeasuresResponse.class)
                .isEqualTo(
                    new MeasuresResponse().metrics(
                        List.of(
                            new MeasuresResponseMetricsInner()
                                .name(MetricName.HTTP_REQUESTS)
                                .measures(List.of(new Measure().name(MeasureName.COUNT).value(42)))
                        )
                    )
                );
        }
    }
}
