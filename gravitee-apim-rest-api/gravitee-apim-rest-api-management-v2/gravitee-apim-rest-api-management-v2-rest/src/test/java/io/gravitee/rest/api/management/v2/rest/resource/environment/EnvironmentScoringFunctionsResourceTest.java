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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static assertions.MAPIAssertions.assertThat;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import fixtures.core.model.ScoringFunctionFixture;
import inmemory.InMemoryAlternative;
import inmemory.ScoringFunctionCrudServiceInMemory;
import io.gravitee.apim.core.scoring.model.ScoringFunction;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.ImportScoringFunction;
import io.gravitee.rest.api.management.v2.rest.model.ScoringFunctionsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EnvironmentScoringFunctionsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";

    WebTarget target;

    @Inject
    ScoringFunctionCrudServiceInMemory scoringFunctionCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/scoring/functions";
    }

    @BeforeEach
    void setup() {
        target = rootTarget();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();
        UuidString.reset();
        GraviteeContext.cleanContext();

        Stream.of(scoringFunctionCrudService).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class ImportEnvironmentFunction {

        @Test
        void should_create_function() {
            // Given
            var request = new ImportScoringFunction().name("function-name.js").payload("function-payload");

            // When
            target.request().post(json(request));

            // Then
            assertThat(scoringFunctionCrudService.storage())
                .extracting(ScoringFunction::name, ScoringFunction::payload)
                .containsExactly(tuple("function-name.js", "function-payload"));
        }

        @Test
        void should_set_location_header_with_created_function_url() {
            // Given
            UuidString.overrideGenerator(() -> "generated-id");
            var request = new ImportScoringFunction().name("function-name.js").payload("function-payload");

            // When
            Response response = target.request().post(json(request));

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.CREATED_201)
                .hasHeader("Location", target.path("generated-id").getUri().toString());
        }

        @Test
        void should_override_function_with_same_name() {
            // Given
            scoringFunctionCrudService.initWith(
                List.of(
                    ScoringFunctionFixture.aFunction().toBuilder().id("function1").name("function-name.js").referenceId(ENVIRONMENT).build()
                )
            );
            var request2 = new ImportScoringFunction().name("function-name.js").payload("function-payload2");

            // When
            Response response = target.request().post(json(request2));

            // Then
            assertThat(response).hasStatus(HttpStatusCode.CREATED_201);
            assertThat(scoringFunctionCrudService.storage())
                .hasSize(1)
                .extracting(ScoringFunction::name, ScoringFunction::payload)
                .containsExactly(tuple("function-name.js", "function-payload2"));
        }
    }

    @Nested
    class ListEnvironmentFunctions {

        @Test
        void should_return_functions() {
            // Given
            scoringFunctionCrudService.initWith(
                List.of(
                    ScoringFunctionFixture.aFunction().toBuilder().id("function1").referenceId(ENVIRONMENT).build(),
                    ScoringFunctionFixture.aFunction().toBuilder().id("function2").referenceId(ENVIRONMENT).build()
                )
            );

            // When
            var response = target.request().get();

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(ScoringFunctionsResponse.class)
                .extracting(ScoringFunctionsResponse::getData)
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactly(
                    new io.gravitee.rest.api.management.v2.rest.model.ScoringFunction()
                        .name("function-name")
                        .payload("function-payload")
                        .referenceId(ENVIRONMENT)
                        .referenceType(io.gravitee.rest.api.management.v2.rest.model.ScoringFunctionReferenceType.ENVIRONMENT)
                        .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atOffset(ZoneOffset.UTC)),
                    new io.gravitee.rest.api.management.v2.rest.model.ScoringFunction()
                        .name("function-name")
                        .payload("function-payload")
                        .referenceId(ENVIRONMENT)
                        .referenceType(io.gravitee.rest.api.management.v2.rest.model.ScoringFunctionReferenceType.ENVIRONMENT)
                        .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atOffset(ZoneOffset.UTC))
                );
        }
    }
}
