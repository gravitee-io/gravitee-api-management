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

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.zee.domain_service.LlmEngineService;
import io.gravitee.apim.core.zee.model.ZeeMetadata;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import io.gravitee.apim.core.zee.model.ZeeResult;
import io.gravitee.rest.api.management.v2.rest.model.ZeeResultDto;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ZeeResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    private LlmEngineService llmEngineService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/ai";
    }

    @BeforeEach
    void setup() {
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT);
        environment.setOrganizationId(ORGANIZATION);

        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environment);
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        reset(llmEngineService);
        GraviteeContext.cleanContext();
    }

    @Nested
    class GenerateResource {

        @Test
        void should_generate_resource_successfully() {
            // Given
            ObjectNode generatedNode = MAPPER.createObjectNode();
            generatedNode.put("name", "My Flow");
            generatedNode.put("enabled", true);

            var llmResult = new LlmEngineService.LlmGenerationResult(generatedNode, true, 42, List.of(), List.of());
            when(llmEngineService.generate(any(String.class), eq("Flow"))).thenReturn(llmResult);

            String requestJson = """
                {
                    "resourceType": "FLOW",
                    "prompt": "Create a flow that rate-limits to 100 req/s",
                    "contextData": {"apiId": "api-123"}
                }
                """;

            FormDataMultiPart multiPart = new FormDataMultiPart();
            multiPart.field("request", requestJson, jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE);

            // When
            final Response response = rootTarget("generate")
                .register(MultiPartFeature.class)
                .request()
                .post(Entity.entity(multiPart, multiPart.getMediaType()));

            // Then
            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(ZeeResultDto.class);
            assertThat(body.getResourceType()).isEqualTo("FLOW");
            assertThat(body.getGenerated()).isNotNull();
            assertThat(body.getGenerated().get("name").asText()).isEqualTo("My Flow");
            assertThat(body.getMetadata()).isNotNull();
            assertThat(body.getMetadata().getModel()).isEqualTo("gpt-4o-mini");
            assertThat(body.getMetadata().getTokensUsed()).isEqualTo(42);
        }

        @Test
        void should_return_500_when_use_case_throws() {
            // Given
            when(llmEngineService.generate(any(String.class), eq("Flow"))).thenThrow(new RuntimeException("LLM service unavailable"));

            String requestJson = """
                {
                    "resourceType": "FLOW",
                    "prompt": "Create a flow",
                    "contextData": {}
                }
                """;

            FormDataMultiPart multiPart = new FormDataMultiPart();
            multiPart.field("request", requestJson, jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE);

            // When
            final Response response = rootTarget("generate")
                .register(MultiPartFeature.class)
                .request()
                .post(Entity.entity(multiPart, multiPart.getMediaType()));

            // Then
            assertThat(response.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR_500);
        }

        @Test
        void should_generate_different_resource_types() {
            // Given
            ObjectNode generatedNode = MAPPER.createObjectNode();
            generatedNode.put("name", "Gold Plan");

            var llmResult = new LlmEngineService.LlmGenerationResult(generatedNode, true, 30, List.of(), List.of());
            when(llmEngineService.generate(any(String.class), eq("Plan"))).thenReturn(llmResult);

            String requestJson = """
                {
                    "resourceType": "PLAN",
                    "prompt": "Create a gold tier plan",
                    "contextData": {}
                }
                """;

            FormDataMultiPart multiPart = new FormDataMultiPart();
            multiPart.field("request", requestJson, jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE);

            // When
            final Response response = rootTarget("generate")
                .register(MultiPartFeature.class)
                .request()
                .post(Entity.entity(multiPart, multiPart.getMediaType()));

            // Then
            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(ZeeResultDto.class);
            assertThat(body.getResourceType()).isEqualTo("PLAN");
            assertThat(body.getGenerated().get("name").asText()).isEqualTo("Gold Plan");
        }
    }
}
