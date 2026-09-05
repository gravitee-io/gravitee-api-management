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
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.observability.filter.inmemory.InMemoryObservabilityFilterDataPort;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValue;
import io.gravitee.gamma.rest.core.observability.filter.use_case.GetObservabilityFilterDefinitionsUseCase;
import io.gravitee.gamma.rest.core.observability.filter.use_case.GetObservabilityFilterValuesUseCase;
import io.gravitee.gamma.rest.core.observability.filter.use_case.ResolveObservabilityFilterLabelsUseCase;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resource.observability.filters.ObservabilityFilterValuesResourceTest.ValuesTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    }

    @Test
    void should_list_entrypoint_values_from_the_data_store() {
        dataPort.givenKeywordValues("ENTRYPOINT", List.of(new FilterValue("http-proxy", null), new FilterValue("mcp-studio", null)));

        Response response = rootTarget("ENTRYPOINT/values").queryParam("apiType", "MCP").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        JsonNode body = response.readEntity(JsonNode.class);
        assertThat(body.get("data"))
            .extracting(node -> node.get("value").asText())
            .containsExactly("http-proxy", "mcp-studio");
        assertThat(body.get("pagination").get("totalCount").asLong()).isEqualTo(2L);
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

    @Test
    void should_answer_400_for_a_page_beyond_the_maximum() {
        Response response = rootTarget("ENTRYPOINT/values").queryParam("page", 10_001).request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        assertThat(dataPort.lastCall()).isEmpty();
    }

    @Configuration
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
        InMemoryObservabilityFilterDataPort observabilityFilterDataPort() {
            return new InMemoryObservabilityFilterDataPort();
        }

        @Bean
        GetObservabilityFilterValuesUseCase getObservabilityFilterValuesUseCase(InMemoryObservabilityFilterDataPort dataPort) {
            return new GetObservabilityFilterValuesUseCase(new SpiFilterRegistry(), dataPort);
        }
    }
}
