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
package io.gravitee.rest.api.management.v2.rest.resource.logs;

import static assertions.MAPIAssertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.management.v2.rest.model.logs.engine.LogsFilterSpec;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.LogsFilterSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LogsDefinitionResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";

    @BeforeEach
    void setup() {
        var environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT);
        environmentEntity.setOrganizationId(ORGANIZATION);

        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/logs";
    }

    @Test
    void should_return_filter_definitions() {
        var response = rootTarget().path("definition").path("filters").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(LogsFilterSpecsResponse.class)
            .extracting(LogsFilterSpecsResponse::getData)
            .satisfies(filters -> assertThat(filters).hasSize(11));
    }

    @Test
    void should_return_filter_definitions_with_correct_structure() {
        var response = rootTarget().path("definition").path("filters").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(LogsFilterSpecsResponse.class)
            .extracting(LogsFilterSpecsResponse::getData)
            .satisfies(filters -> {
                var apiFilter = filters
                    .stream()
                    .filter(f -> f.getName().getValue().equals("API"))
                    .findFirst()
                    .orElseThrow();
                assertThat(apiFilter.getLabel()).isEqualTo("API");
                assertThat(apiFilter.getType()).isEqualTo(LogsFilterSpec.TypeEnum.KEYWORD);
                assertThat(apiFilter.getOperators()).isNotEmpty();

                var responseTimeFilter = filters
                    .stream()
                    .filter(f -> f.getName().getValue().equals("RESPONSE_TIME"))
                    .findFirst()
                    .orElseThrow();
                assertThat(responseTimeFilter.getLabel()).isEqualTo("Response Time");
                assertThat(responseTimeFilter.getType()).isEqualTo(LogsFilterSpec.TypeEnum.NUMBER);
                assertThat(responseTimeFilter.getOperators()).isNotEmpty();

                var httpStatusFilter = filters
                    .stream()
                    .filter(f -> f.getName().getValue().equals("HTTP_STATUS"))
                    .findFirst()
                    .orElseThrow();
                assertThat(httpStatusFilter.getRange()).isNotNull();
                assertThat(httpStatusFilter.getRange().getMin()).isEqualTo(100);
                assertThat(httpStatusFilter.getRange().getMax()).isEqualTo(599);
            });
    }
}
