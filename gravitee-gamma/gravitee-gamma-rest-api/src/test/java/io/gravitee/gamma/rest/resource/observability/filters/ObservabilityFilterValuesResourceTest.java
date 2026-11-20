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
package io.gravitee.gamma.rest.resource.observability.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.observability.model.AdditionalMetricField;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.InMemoryAdditionalMetricDefinitionRepository;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDefinition;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.EndUserDisplayDomainService;
import io.gravitee.gamma.rest.core.observability.filter.inmemory.InMemoryObservabilityFilterDataPort;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValue;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.core.observability.filter.use_case.GetObservabilityFilterDefinitionsUseCase;
import io.gravitee.gamma.rest.core.observability.filter.use_case.GetObservabilityFilterValuesUseCase;
import io.gravitee.gamma.rest.core.observability.filter.use_case.ResolveObservabilityFilterLabelsUseCase;
import io.gravitee.gamma.rest.infra.adapter.AdditionalMetricGrammarPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resource.observability.filters.ObservabilityFilterValuesResourceTest.ValuesTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * Drives real filter-value requests through the resource, the real use case and the real catalog,
 * with only the data store replaced: what a caller gets for a filter the catalog offers, and for one
 * whose values the store cannot list.
 */
@ContextConfiguration(classes = { ResourceContextConfiguration.class, ValuesTestConfiguration.class })
class ObservabilityFilterValuesResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";

    @Inject
    private InMemoryObservabilityFilterDataPort dataPort;

    @Inject
    private InMemoryAdditionalMetricDefinitionRepository records;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/observability/filters";
    }

    @BeforeEach
    void prepareEnvironment() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId(ENVIRONMENT);
        env.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(env);
    }

    @AfterEach
    void resetStore() {
        dataPort.reset();
        records.reset();
    }

    @Test
    void should_list_entrypoint_values_from_the_data_store() {
        dataPort.givenKeywordValues("ENTRYPOINT", List.of(new FilterValue("http-proxy", null), new FilterValue("mcp-studio", null)));

        Response response = rootTarget("ENTRYPOINT/values").queryParam("apiType", "MCP").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        JsonNode body = response.readEntity(JsonNode.class);
        assertThat(body.get("data"))
            .extracting(node -> node.get("value").asText())
            .containsExactly("http-proxy", "mcp-studio", "(none)");
        assertThat(body.get("pagination").get("totalCount").asLong()).isEqualTo(3L);
        assertThat(dataPort.lastCall()).hasValueSatisfying(call -> assertThat(call.filterName()).isEqualTo("ENTRYPOINT"));
    }

    @Test
    void should_answer_400_for_a_keyword_filter_whose_values_the_store_cannot_list() {
        // A Kafka topic name is unbounded and lives in the event-metrics stream the store does not
        // aggregate over: the catalog offers the filter, not its values, and says so instead of failing.
        Response response = rootTarget("NATIVE_TOPIC/values").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        JsonNode body = response.readEntity(JsonNode.class);
        assertThat(body.get("message").asText()).contains("NATIVE_TOPIC");
        assertThat(dataPort.lastCall()).isEmpty();
    }

    /** E2E finding C2: a name breaking the field grammar is a 400 naming the rule, not a 404, on both forms. */
    @Test
    void should_answer_400_naming_the_rule_for_a_custom_name_outside_the_grammar() {
        for (var response : List.of(
            rootTarget("custom.a..b/values").request().get(),
            rootTarget("values").queryParam("name", "custom.a..b").request().get()
        )) {
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("technicalCode").asText()).isEqualTo("observability.filter.invalid_name");
            assertThat(body.get("message").asText()).isEqualTo(
                "Filter 'custom.a..b' is not a valid field name: " + AdditionalMetricField.CUSTOM_NAME_RULE
            );
        }
        assertThat(dataPort.lastCall()).isEmpty();
    }

    /** Hiding a key from the pickers is not a redaction: its values are listed the same, exposed or not. */
    @Test
    void should_list_the_values_of_a_hidden_custom_key_as_when_it_is_exposed() {
        dataPort.givenKeywordValues("custom.fraud-tier", List.of(new FilterValue("gold", null), new FilterValue("silver", null)));
        records.create(fraudTierRecord(true));
        JsonNode exposed = rootTarget("values").queryParam("name", "custom.fraud-tier").request().get().readEntity(JsonNode.class);
        records.reset();
        records.create(fraudTierRecord(false));

        Response hidden = rootTarget("values").queryParam("name", "custom.fraud-tier").request().get();

        assertThat(hidden.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        JsonNode body = hidden.readEntity(JsonNode.class);
        assertThat(body.get("data"))
            .extracting(node -> node.get("value").asText())
            .contains("gold", "silver");
        assertThat(body).isEqualTo(exposed);
    }

    private static AdditionalMetricDefinition fraudTierRecord(boolean exposed) {
        return new AdditionalMetricDefinition(
            "fraud-tier",
            ENVIRONMENT,
            new AdditionalMetricGrammarPortAdapter().parse("custom.fraud-tier").orElseThrow(),
            "Fraud tier",
            null,
            null,
            exposed,
            Instant.EPOCH,
            Instant.EPOCH
        );
    }

    /** The test organization leaves {@code logging.user.displayed} off. */
    @Test
    void should_answer_400_to_the_end_user_values_while_the_organization_does_not_display_it() {
        Response response = rootTarget("USER/values").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        assertThat(response.readEntity(JsonNode.class).get("technicalCode").asText()).isEqualTo(
            "observability.filter.end_user_not_displayed"
        );
        assertThat(dataPort.lastCall()).isEmpty();
    }

    @Test
    void should_answer_400_to_the_end_user_values_by_name_while_the_organization_does_not_display_it() {
        Response response = rootTarget("values").queryParam("name", "USER").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        assertThat(response.readEntity(JsonNode.class).get("technicalCode").asText()).isEqualTo(
            "observability.filter.end_user_not_displayed"
        );
        assertThat(dataPort.lastCall()).isEmpty();
    }

    @Test
    void should_still_answer_404_for_a_name_under_no_dynamic_root() {
        Response response = rootTarget("UNKNOWN/values").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    void should_answer_400_for_a_page_beyond_the_maximum() {
        Response response = rootTarget("ENTRYPOINT/values").queryParam("page", 10_001).request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        assertThat(dataPort.lastCall()).isEmpty();
    }

    /**
     * Spring builds the values use case, so a port it may come to need is resolved from this context, where the record
     * store holding a hidden key is the only one.
     */
    @Configuration
    @Import(GetObservabilityFilterValuesUseCase.class)
    static class ValuesTestConfiguration {

        @Bean
        GetObservabilityFilterDefinitionsUseCase getObservabilityFilterDefinitionsUseCase() {
            return mock(GetObservabilityFilterDefinitionsUseCase.class);
        }

        @Bean
        ResolveObservabilityFilterLabelsUseCase resolveObservabilityFilterLabelsUseCase() {
            return mock(ResolveObservabilityFilterLabelsUseCase.class);
        }

        @Bean
        InMemoryAdditionalMetricDefinitionRepository additionalMetricDefinitionRepository() {
            return new InMemoryAdditionalMetricDefinitionRepository();
        }

        @Bean
        InMemoryObservabilityFilterDataPort observabilityFilterDataPort() {
            return new InMemoryObservabilityFilterDataPort();
        }

        @Bean
        FilterRegistry filterRegistry() {
            return new SpiFilterRegistry();
        }

        @Bean
        EndUserDisplayDomainService endUserDisplayDomainService() {
            return new EndUserDisplayDomainService(organizationId -> false);
        }
    }
}
