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
package io.gravitee.gamma.rest.resource.observability.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityFacetsUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityMeasuresUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityTimeSeriesUseCase;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resource.observability.analytics.AnalyticsResourceAuthorizationTest.AnalyticsAuthorizationTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { ResourceContextConfiguration.class, AnalyticsAuthorizationTestConfiguration.class })
class AnalyticsResourceAuthorizationTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";
    private static final String OTHER_ENVIRONMENT = "other-env";

    @Inject
    private ComputeObservabilityMeasuresUseCase computeMeasuresUseCase;

    @Inject
    private ComputeObservabilityFacetsUseCase computeFacetsUseCase;

    @Inject
    private ComputeObservabilityTimeSeriesUseCase computeTimeSeriesUseCase;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/observability/analytics";
    }

    @BeforeEach
    void prepareEnvironment() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId(ENVIRONMENT);
        env.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(env);
        when(computeMeasuresUseCase.execute(any())).thenReturn(
            new ComputeObservabilityMeasuresUseCase.Output(JsonNodeFactory.instance.objectNode())
        );
        when(computeFacetsUseCase.execute(any())).thenReturn(
            new ComputeObservabilityFacetsUseCase.Output(JsonNodeFactory.instance.objectNode())
        );
        when(computeTimeSeriesUseCase.execute(any())).thenReturn(
            new ComputeObservabilityTimeSeriesUseCase.Output(JsonNodeFactory.instance.objectNode())
        );
    }

    @AfterEach
    void resetUseCases() {
        reset(computeMeasuresUseCase, computeFacetsUseCase, computeTimeSeriesUseCase);
    }

    static Stream<Arguments> endpoints() {
        return Stream.of(arguments("measures"), arguments("facets"), arguments("time-series"));
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    void should_answer_403_without_computing_when_caller_only_holds_environment_api_read(String path) {
        givenOnlyReadPermission(RolePermission.ENVIRONMENT_API, ENVIRONMENT);

        Response response = post(path);

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verifyNoInteractions(computeMeasuresUseCase, computeFacetsUseCase, computeTimeSeriesUseCase);
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    void should_answer_403_when_environment_platform_read_is_held_on_another_environment(String path) {
        givenOnlyReadPermission(RolePermission.ENVIRONMENT_PLATFORM, OTHER_ENVIRONMENT);

        Response response = post(path);

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verifyNoInteractions(computeMeasuresUseCase, computeFacetsUseCase, computeTimeSeriesUseCase);
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    void should_answer_200_when_caller_holds_environment_platform_read(String path) {
        givenOnlyReadPermission(RolePermission.ENVIRONMENT_PLATFORM, ENVIRONMENT);

        Response response = post(path);

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
    }

    private Response post(String path) {
        return rootTarget(path).request().post(Entity.entity(Map.of(), MediaType.APPLICATION_JSON_TYPE));
    }

    @Configuration
    static class AnalyticsAuthorizationTestConfiguration {

        @Bean
        ComputeObservabilityMeasuresUseCase computeMeasuresUseCase() {
            return mock(ComputeObservabilityMeasuresUseCase.class);
        }

        @Bean
        ComputeObservabilityFacetsUseCase computeFacetsUseCase() {
            return mock(ComputeObservabilityFacetsUseCase.class);
        }

        @Bean
        ComputeObservabilityTimeSeriesUseCase computeTimeSeriesUseCase() {
            return mock(ComputeObservabilityTimeSeriesUseCase.class);
        }
    }
}
