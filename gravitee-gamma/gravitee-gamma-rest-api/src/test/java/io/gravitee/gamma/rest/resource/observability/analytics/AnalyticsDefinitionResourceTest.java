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

import static io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinitionFixtures.aMetric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityFacetsUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityMeasuresUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityTimeSeriesUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.GetObservabilityAnalyticsDefinitionUseCase;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resource.observability.analytics.AnalyticsDefinitionResourceTest.AnalyticsDefinitionTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { ResourceContextConfiguration.class, AnalyticsDefinitionTestConfiguration.class })
class AnalyticsDefinitionResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";

    @Inject
    private GetObservabilityAnalyticsDefinitionUseCase getAnalyticsDefinitionUseCase;

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
    }

    @AfterEach
    void resetUseCase() {
        reset(getAnalyticsDefinitionUseCase);
    }

    @Test
    void should_return_the_families_with_facets_named_and_their_labels_in_dimensions() {
        givenDefinition(
            aMetric("HTTP_REQUESTS", List.of(ApiType.HTTP_PROXY, ApiType.LLM), "API"),
            aMetric("LLM_PROMPT_TOTAL_TOKEN", List.of(ApiType.LLM), "API", "LLM_PROXY_MODEL")
        );

        Response response = rootTarget("definition").queryParam("apiType", "LLM").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        JsonNode body = response.readEntity(JsonNode.class);
        assertThat(body.get("families"))
            .extracting(family -> family.get("name").asText())
            .containsExactly("GATEWAY", "LLM");
        assertThat(body.get("families").get(0).get("label").asText()).isEqualTo("Gateway");
        JsonNode llmMetric = body.get("families").get(1).get("metrics").get(0);
        assertThat(llmMetric.get("name").asText()).isEqualTo("LLM_PROMPT_TOTAL_TOKEN");
        assertThat(llmMetric.get("label").asText()).isEqualTo("LLM_PROMPT_TOTAL_TOKEN label");
        assertThat(llmMetric.get("unit").asText()).isEqualTo("NUMBER");
        assertThat(llmMetric.get("apiTypes")).extracting(JsonNode::asText).containsExactly("LLM");
        assertThat(llmMetric.get("measures")).extracting(JsonNode::asText).containsExactly("COUNT", "AVG");
        assertThat(llmMetric.get("facets")).extracting(JsonNode::asText).containsExactly("API", "LLM_PROXY_MODEL");
        assertThat(llmMetric.get("filters")).extracting(JsonNode::asText).containsExactly("API", "API_TYPE");
        assertThat(llmMetric.get("origin").asText()).isEqualTo("BUILT_IN");
        assertThat(body.get("dimensions")).hasSize(2);
        assertThat(body.get("dimensions").get(1).get("name").asText()).isEqualTo("LLM_PROXY_MODEL");
        assertThat(body.get("dimensions").get(1).get("label").asText()).isEqualTo("LLM_PROXY_MODEL label");
        assertThat(body.get("dimensions").get(1).get("origin").asText()).isEqualTo("BUILT_IN");
    }

    @Test
    void should_forward_multi_valued_case_insensitive_api_types_to_the_use_case() {
        givenDefinition();

        Response response = rootTarget("definition").queryParam("apiType", "HTTP_PROXY").queryParam("apiType", "authz").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var captor = ArgumentCaptor.forClass(GetObservabilityAnalyticsDefinitionUseCase.Input.class);
        verify(getAnalyticsDefinitionUseCase).execute(captor.capture());
        assertThat(captor.getValue().apiTypes()).containsExactlyInAnyOrder(ApiType.HTTP_PROXY, ApiType.AUTHZ);
    }

    @Test
    void should_return_400_with_the_technical_code_for_an_unknown_api_type() {
        Response response = rootTarget("definition").queryParam("apiType", "NOPE").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        JsonNode body = response.readEntity(JsonNode.class);
        assertThat(body.get("technicalCode").asText()).isEqualTo("observability.query.not_an_api_kind");
        assertThat(body.get("message").asText()).isEqualTo("'NOPE' is not an API kind");
        verifyNoInteractions(getAnalyticsDefinitionUseCase);
    }

    @Test
    void should_return_400_with_the_technical_code_when_the_use_case_refuses_the_api_types() {
        when(getAnalyticsDefinitionUseCase.execute(any())).thenThrow(InvalidObservabilityQueryException.missingApiTypes());

        Response response = rootTarget("definition").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        assertThat(response.readEntity(JsonNode.class).get("technicalCode").asText()).isEqualTo("observability.query.api_types_required");
    }

    @Test
    void should_tag_the_response_and_ask_the_client_to_revalidate_it() {
        givenDefinition(aMetric("HTTP_REQUESTS", List.of(ApiType.HTTP_PROXY), "API"));

        Response response = rootTarget("definition").queryParam("apiType", "HTTP_PROXY").request().get();

        assertThat(response.getEntityTag()).isNotNull();
        assertThat(response.getHeaderString(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-cache");
    }

    @Test
    void should_answer_the_same_definition_with_the_same_entity_tag_and_another_one_with_another() {
        givenDefinition(aMetric("HTTP_REQUESTS", List.of(ApiType.HTTP_PROXY), "API"));
        var first = rootTarget("definition").queryParam("apiType", "HTTP_PROXY").request().get().getEntityTag();
        var again = rootTarget("definition").queryParam("apiType", "HTTP_PROXY").request().get().getEntityTag();
        givenDefinition(aMetric("HTTP_ERRORS", List.of(ApiType.HTTP_PROXY), "API"));
        var other = rootTarget("definition").queryParam("apiType", "HTTP_PROXY").request().get().getEntityTag();

        assertThat(again).isEqualTo(first);
        assertThat(other).isNotEqualTo(first);
    }

    @Test
    void should_answer_304_without_a_body_when_the_client_holds_the_current_definition() {
        givenDefinition(aMetric("HTTP_REQUESTS", List.of(ApiType.HTTP_PROXY), "API"));
        var tag = rootTarget("definition").queryParam("apiType", "HTTP_PROXY").request().get().getEntityTag();

        Response response = rootTarget("definition")
            .queryParam("apiType", "HTTP_PROXY")
            .request()
            .header(HttpHeaders.IF_NONE_MATCH, tag.toString())
            .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_MODIFIED_304);
        assertThat(response.hasEntity()).isFalse();
        assertThat(response.getEntityTag()).isEqualTo(tag);
    }

    /** The definition holds no traffic data. */
    @Test
    void should_return_the_definition_to_a_caller_without_any_permission() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        givenDefinition();

        Response response = rootTarget("definition").queryParam("apiType", "HTTP_PROXY").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
    }

    private void givenDefinition(MetricDefinition... metrics) {
        when(getAnalyticsDefinitionUseCase.execute(any())).thenReturn(
            new GetObservabilityAnalyticsDefinitionUseCase.Output(AnalyticsDefinition.of(List.of(metrics)))
        );
    }

    @Configuration
    static class AnalyticsDefinitionTestConfiguration {

        // Needed by AnalyticsResource, the parent the definition is located through.
        @Bean
        ComputeObservabilityMeasuresUseCase computeObservabilityMeasuresUseCase() {
            return mock(ComputeObservabilityMeasuresUseCase.class);
        }

        @Bean
        ComputeObservabilityFacetsUseCase computeObservabilityFacetsUseCase() {
            return mock(ComputeObservabilityFacetsUseCase.class);
        }

        @Bean
        ComputeObservabilityTimeSeriesUseCase computeObservabilityTimeSeriesUseCase() {
            return mock(ComputeObservabilityTimeSeriesUseCase.class);
        }

        @Bean
        GetObservabilityAnalyticsDefinitionUseCase getObservabilityAnalyticsDefinitionUseCase() {
            return mock(GetObservabilityAnalyticsDefinitionUseCase.class);
        }
    }
}
