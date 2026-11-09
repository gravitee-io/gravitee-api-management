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
import io.gravitee.gamma.rest.core.observability.analytics.model.DimensionDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityFacetsUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityMeasuresUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityTimeSeriesUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.GetObservabilityAnalyticsDefinitionUseCase;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resource.observability.analytics.AnalyticsDefinitionResourceTest.AnalyticsDefinitionTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
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
    private static final String OTHER_ENVIRONMENT = "other-env";

    @Inject
    private GetObservabilityAnalyticsDefinitionUseCase getAnalyticsDefinitionUseCase;

    private final Client otherEnvironmentClient = ClientBuilder.newClient();

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
            aMetric("LLM_PROMPT_TOTAL_TOKEN", "TOKENS", List.of(ApiType.LLM), "API", "LLM_PROXY_MODEL")
        );

        Response response = rootTarget("definition").queryParam("apiType", "LLM").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        JsonNode body = response.readEntity(JsonNode.class);
        assertThat(body.get("families"))
            .extracting(family -> family.get("name").asText())
            .containsExactly("GATEWAY", "LLM");
        assertThat(body.get("truncated").asBoolean()).isFalse();
        assertThat(body.get("families").get(0).get("label").asText()).isEqualTo("Gateway");
        JsonNode llmMetric = body.get("families").get(1).get("metrics").get(0);
        assertThat(llmMetric.get("name").asText()).isEqualTo("LLM_PROMPT_TOTAL_TOKEN");
        assertThat(llmMetric.get("label").asText()).isEqualTo("LLM_PROMPT_TOTAL_TOKEN label");
        assertThat(llmMetric.get("unit").asText()).isEqualTo("TOKENS");
        assertThat(llmMetric.get("apiTypes")).extracting(JsonNode::asText).containsExactly("LLM");
        assertThat(llmMetric.get("measures")).extracting(JsonNode::asText).containsExactly("COUNT", "AVG");
        assertThat(llmMetric.get("facets")).extracting(JsonNode::asText).containsExactly("API", "LLM_PROXY_MODEL");
        assertThat(llmMetric.get("filters")).extracting(JsonNode::asText).containsExactly("API", "API_TYPE");
        assertThat(llmMetric.get("origin").asText()).isEqualTo("BUILT_IN");
        assertThat(body.get("dimensions")).hasSize(2);
        assertThat(body.get("dimensions").get(1).get("name").asText()).isEqualTo("LLM_PROXY_MODEL");
        assertThat(body.get("dimensions").get(1).get("label").asText()).isEqualTo("LLM_PROXY_MODEL label");
        assertThat(body.get("dimensions").get(1).get("origin").asText()).isEqualTo("BUILT_IN");
        assertThat(body.get("dimensions").get(1).get("apiTypes")).isEmpty();
        assertThat(body.get("dimensions").get(1).has("namespace")).isFalse();
    }

    @Test
    void should_forward_the_environment_and_the_multi_valued_case_insensitive_api_types_to_the_use_case() {
        givenDefinition();

        Response response = rootTarget("definition").queryParam("apiType", "HTTP_PROXY").queryParam("apiType", "authz").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var captor = ArgumentCaptor.forClass(GetObservabilityAnalyticsDefinitionUseCase.Input.class);
        verify(getAnalyticsDefinitionUseCase).execute(captor.capture());
        assertThat(captor.getValue().organizationId()).isEqualTo(ORGANIZATION);
        assertThat(captor.getValue().environmentId()).isEqualTo(ENVIRONMENT);
        assertThat(captor.getValue().apiTypes()).containsExactlyInAnyOrder(ApiType.HTTP_PROXY, ApiType.AUTHZ);
        assertThat(captor.getValue().origins()).isEmpty();
    }

    @Test
    void should_forward_the_multi_valued_case_insensitive_origins_to_the_use_case() {
        givenDefinition();

        Response response = rootTarget("definition")
            .queryParam("apiType", "LLM")
            .queryParam("origin", "declared")
            .queryParam("origin", "DISCOVERED")
            .request()
            .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var captor = ArgumentCaptor.forClass(GetObservabilityAnalyticsDefinitionUseCase.Input.class);
        verify(getAnalyticsDefinitionUseCase).execute(captor.capture());
        assertThat(captor.getValue().origins()).containsExactlyInAnyOrder(CatalogOrigin.DECLARED, CatalogOrigin.DISCOVERED);
    }

    @Test
    void should_return_400_for_an_unknown_origin() {
        Response response = rootTarget("definition").queryParam("apiType", "LLM").queryParam("origin", "NOPE").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        assertThat(response.readEntity(JsonNode.class).get("message").asText()).isEqualTo("Unknown origin value 'NOPE'");
        verifyNoInteractions(getAnalyticsDefinitionUseCase);
    }

    @Test
    void should_serve_the_namespace_and_description_of_an_additional_metric_and_omit_them_when_unknown() {
        givenDefinition(
            new MetricDefinition(
                "additional-metrics.long_llm-proxy_tokens-cached",
                "Cached tokens",
                "Tokens served from the cache",
                "llm-proxy",
                null,
                List.of(ApiType.LLM),
                List.of("AVG", "COUNT"),
                List.of(
                    new DimensionDefinition(
                        "additional-metrics.bool_pii_detected",
                        "Detected",
                        "pii",
                        new LinkedHashSet<>(List.of(ApiType.LLM, ApiType.HTTP_PROXY)),
                        CatalogOrigin.DISCOVERED
                    ),
                    new DimensionDefinition("additional-metrics.keyword_fraud-check_tier", "Fraud check tier", CatalogOrigin.DISCOVERED)
                ),
                List.of("API"),
                CatalogOrigin.DECLARED,
                null
            ),
            anAdditionalMetric("additional-metrics.long_failover_count", "Failover count", null, CatalogOrigin.DISCOVERED)
        );

        JsonNode body = rootTarget("definition").queryParam("apiType", "LLM").request().get().readEntity(JsonNode.class);

        JsonNode cached = body.get("families").get(0).get("metrics").get(0);
        assertThat(cached.get("namespace").asText()).isEqualTo("llm-proxy");
        assertThat(cached.get("description").asText()).isEqualTo("Tokens served from the cache");
        assertThat(cached.get("apiTypes")).extracting(JsonNode::asText).containsExactly("LLM");
        assertThat(cached.get("facets"))
            .extracting(JsonNode::asText)
            .containsExactly("additional-metrics.bool_pii_detected", "additional-metrics.keyword_fraud-check_tier");
        JsonNode failover = body.get("families").get(0).get("metrics").get(1);
        assertThat(failover.has("namespace")).isFalse();
        assertThat(failover.has("description")).isFalse();
        JsonNode dimension = body.get("dimensions").get(0);
        assertThat(dimension.get("name").asText()).isEqualTo("additional-metrics.bool_pii_detected");
        assertThat(dimension.get("namespace").asText()).isEqualTo("pii");
        assertThat(dimension.get("apiTypes"))
            .as("sorted, whatever order the kinds are held in")
            .extracting(JsonNode::asText)
            .containsExactly("HTTP_PROXY", "LLM");
        assertThat(dimension.get("origin").asText()).isEqualTo("DISCOVERED");
        JsonNode unscoped = body.get("dimensions").get(1);
        assertThat(unscoped.get("name").asText()).isEqualTo("additional-metrics.keyword_fraud-check_tier");
        assertThat(unscoped.has("namespace")).isFalse();
        assertThat(unscoped.get("apiTypes")).isEmpty();
    }

    @Test
    void should_serve_the_additional_metrics_with_their_unit_when_known_and_say_when_the_discovery_was_truncated() {
        when(getAnalyticsDefinitionUseCase.execute(any())).thenReturn(
            new GetObservabilityAnalyticsDefinitionUseCase.Output(
                AnalyticsDefinition.of(
                    List.of(
                        anAdditionalMetric("additional-metrics.double_checkout_basket-value", "Basket value", "€", CatalogOrigin.DECLARED),
                        anAdditionalMetric("additional-metrics.long_failover_count", "Failover count", null, CatalogOrigin.DISCOVERED)
                    )
                ),
                true
            )
        );

        JsonNode body = rootTarget("definition").queryParam("apiType", "HTTP_PROXY").request().get().readEntity(JsonNode.class);

        assertThat(body.get("truncated").asBoolean()).isTrue();
        JsonNode family = body.get("families").get(0);
        assertThat(family.get("name").asText()).isEqualTo("ADDITIONAL");
        assertThat(family.get("label").asText()).isEqualTo("Additional metrics");
        JsonNode declared = family.get("metrics").get(0);
        assertThat(declared.get("unit").asText()).isEqualTo("€");
        assertThat(declared.get("origin").asText()).isEqualTo("DECLARED");
        assertThat(declared.get("measures")).extracting(JsonNode::asText).containsExactly("AVG", "COUNT");
        JsonNode discovered = family.get("metrics").get(1);
        assertThat(discovered.has("unit")).isFalse();
        assertThat(discovered.get("origin").asText()).isEqualTo("DISCOVERED");
    }

    @Test
    void should_serve_the_currency_code_of_a_cost_next_to_its_unit_and_none_elsewhere() {
        givenDefinition(
            new MetricDefinition(
                "LLM_PROMPT_TOKEN_TOTAL_COST",
                "Total token cost",
                "CURRENCY",
                List.of(ApiType.LLM),
                List.of("COUNT", "AVG"),
                List.of(),
                List.of("API"),
                CatalogOrigin.BUILT_IN,
                "USD"
            ),
            anAdditionalMetric("additional-metrics.double_basket", "Basket", "CURRENCY", CatalogOrigin.DECLARED)
        );

        JsonNode body = rootTarget("definition").queryParam("apiType", "LLM").request().get().readEntity(JsonNode.class);

        var metrics = body.findParents("unit");
        assertThat(metrics)
            .extracting(metric -> metric.get("name").asText())
            .containsExactly("LLM_PROMPT_TOKEN_TOTAL_COST", "additional-metrics.double_basket");
        assertThat(metrics.get(0).get("currency").asText()).isEqualTo("USD");
        assertThat(metrics.get(1).has("currency")).isFalse();
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

    /** The body depends on the caller's traffic right: a shared cache must never serve it to another caller. */
    @Test
    void should_tag_the_response_and_ask_the_client_to_revalidate_it_without_sharing_it() {
        givenDefinition(aMetric("HTTP_REQUESTS", List.of(ApiType.HTTP_PROXY), "API"));

        Response response = rootTarget("definition").queryParam("apiType", "HTTP_PROXY").request().get();

        assertThat(response.getEntityTag()).isNotNull();
        assertThat(response.getHeaderString(HttpHeaders.CACHE_CONTROL)).isEqualTo("private, no-cache");
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

    /** The same built-ins, with and without the discovered entries a traffic reader gets, are two bodies: two tags. */
    @Test
    void should_tag_the_definition_of_a_traffic_reader_differently_from_the_one_of_a_login_only_caller() {
        var builtIn = aMetric("HTTP_REQUESTS", List.of(ApiType.HTTP_PROXY), "API");
        var discovered = anAdditionalMetric("additional-metrics.long_failover_count", "Failover count", null, CatalogOrigin.DISCOVERED);
        givenDefinition(builtIn);
        var loginOnly = rootTarget("definition").queryParam("apiType", "HTTP_PROXY").request().get().getEntityTag();
        givenDefinition(builtIn, discovered);
        var reader = rootTarget("definition").queryParam("apiType", "HTTP_PROXY").request().get().getEntityTag();

        assertThat(reader).isNotEqualTo(loginOnly);
        Response revalidated = rootTarget("definition")
            .queryParam("apiType", "HTTP_PROXY")
            .request()
            .header(HttpHeaders.IF_NONE_MATCH, loginOnly.toString())
            .get();
        assertThat(revalidated.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        assertThat(revalidated.getEntityTag()).isEqualTo(reader);
    }

    @Test
    void should_tag_the_definition_of_each_environment_with_its_own_tag() {
        EnvironmentEntity other = new EnvironmentEntity();
        other.setId(OTHER_ENVIRONMENT);
        other.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, OTHER_ENVIRONMENT)).thenReturn(other);
        when(getAnalyticsDefinitionUseCase.execute(any())).thenAnswer(invocation -> {
            var input = invocation.getArgument(0, GetObservabilityAnalyticsDefinitionUseCase.Input.class);
            var metric = anAdditionalMetric(
                "additional-metrics.double_" + input.environmentId(),
                input.environmentId(),
                null,
                CatalogOrigin.DECLARED
            );
            return new GetObservabilityAnalyticsDefinitionUseCase.Output(AnalyticsDefinition.of(List.of(metric)), false);
        });

        var ours = rootTarget("definition").queryParam("apiType", "HTTP_PROXY").request().get().getEntityTag();
        var theirs = environmentTarget(OTHER_ENVIRONMENT).queryParam("apiType", "HTTP_PROXY").request().get().getEntityTag();

        assertThat(ours).isNotNull();
        assertThat(theirs).isNotNull().isNotEqualTo(ours);
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

    /** A login is enough: the built-ins need no permission; the discovered entries are gated in the use case. */
    @Test
    void should_return_the_definition_to_a_caller_without_any_permission() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        givenDefinition();

        Response response = rootTarget("definition").queryParam("apiType", "HTTP_PROXY").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
    }

    private void givenDefinition(MetricDefinition... metrics) {
        when(getAnalyticsDefinitionUseCase.execute(any())).thenReturn(
            new GetObservabilityAnalyticsDefinitionUseCase.Output(AnalyticsDefinition.of(List.of(metrics)), false)
        );
    }

    private static MetricDefinition anAdditionalMetric(String name, String label, String unit, CatalogOrigin origin) {
        return new MetricDefinition(
            name,
            label,
            unit,
            List.of(ApiType.HTTP_PROXY, ApiType.LLM),
            List.of("AVG", "COUNT"),
            List.of(new DimensionDefinition("API", "API label", CatalogOrigin.BUILT_IN)),
            List.of("API"),
            origin
        );
    }

    /** The harness targets one environment: another one is reached through the same server under its own path. */
    private WebTarget environmentTarget(String environment) {
        var ours = rootTarget("definition").getUri().toString();
        return otherEnvironmentClient.target(ours.replace("/environments/" + ENVIRONMENT + "/", "/environments/" + environment + "/"));
    }

    @AfterAll
    void closeOtherEnvironmentClient() {
        otherEnvironmentClient.close();
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
