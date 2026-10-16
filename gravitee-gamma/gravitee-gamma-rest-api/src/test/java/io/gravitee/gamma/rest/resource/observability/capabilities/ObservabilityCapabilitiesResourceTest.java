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
package io.gravitee.gamma.rest.resource.observability.capabilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.observability.capabilities.model.ObservabilityCapabilities;
import io.gravitee.gamma.rest.core.observability.capabilities.use_case.GetObservabilityCapabilitiesUseCase;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resource.observability.capabilities.ObservabilityCapabilitiesResourceTest.CapabilitiesTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { ResourceContextConfiguration.class, CapabilitiesTestConfiguration.class })
class ObservabilityCapabilitiesResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";

    @Inject
    private GetObservabilityCapabilitiesUseCase getCapabilitiesUseCase;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/observability/capabilities";
    }

    @BeforeEach
    void prepareEnvironment() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId(ENVIRONMENT);
        env.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(env);
    }

    @AfterEach
    void resetUseCase() {
        reset(getCapabilitiesUseCase);
    }

    @Test
    void should_answer_the_capabilities_with_the_keys_of_the_observability_library() {
        when(getCapabilitiesUseCase.execute(any())).thenReturn(
            new GetObservabilityCapabilitiesUseCase.Output(new ObservabilityCapabilities(true, true, true, false))
        );

        var response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var body = response.readEntity(JsonNode.class);
        assertThat(body.get("observability.logs.read").asBoolean()).isTrue();
        assertThat(body.get("observability.traces.read").asBoolean()).isTrue();
        assertThat(body.get("observability.dashboards.read").asBoolean()).isTrue();
        assertThat(body.get("observability.dashboards.write").asBoolean()).isFalse();
        assertThat(body.size()).isEqualTo(4);
    }

    @Test
    void should_ask_for_the_capabilities_of_the_environment_of_the_url_without_any_permission() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(getCapabilitiesUseCase.execute(any())).thenReturn(
            new GetObservabilityCapabilitiesUseCase.Output(new ObservabilityCapabilities(false, false, false, false))
        );

        var response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var captor = ArgumentCaptor.forClass(GetObservabilityCapabilitiesUseCase.Input.class);
        Mockito.verify(getCapabilitiesUseCase).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new GetObservabilityCapabilitiesUseCase.Input(ORGANIZATION, ENVIRONMENT));
    }

    @Configuration
    static class CapabilitiesTestConfiguration {

        @Bean
        GetObservabilityCapabilitiesUseCase getObservabilityCapabilitiesUseCase() {
            return mock(GetObservabilityCapabilitiesUseCase.class);
        }
    }
}
