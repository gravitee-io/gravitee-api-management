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
package io.gravitee.apim.core.api_product.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import inmemory.EventLatestQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.rest.api.model.EventType;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductDeploymentStateDomainServiceTest {

    private static final String ENV_ID = "env-id";
    private static final Instant DEPLOYED_AT = Instant.parse("2024-01-10T10:00:00Z");

    private final EventLatestQueryServiceInMemory eventLatestQueryService = spy(new EventLatestQueryServiceInMemory());
    private final PlanQueryServiceInMemory planQueryService = spy(new PlanQueryServiceInMemory());
    private final ObjectMapper objectMapper = GraviteeJacksonMapper.getInstance();

    private final ApiProductDeploymentStateDomainService service = new ApiProductDeploymentStateDomainService(
        eventLatestQueryService,
        planQueryService,
        objectMapper
    );

    @AfterEach
    void tearDown() {
        eventLatestQueryService.reset();
        planQueryService.reset();
    }

    @Test
    void should_cost_the_same_two_queries_however_many_products_there_are() throws Exception {
        // A query per product would make a page of twenty-five cost fifty round trips, and a listing's cost
        // grow with the environment.
        var products = IntStream.range(0, 25)
            .mapToObj(i -> aProduct("id-" + i))
            .toList();
        eventLatestQueryService.initWith(products.stream().map(this::aMatchingDeployEventFor).toList());

        service.computeDeploymentState(products);

        verify(eventLatestQueryService, times(1)).findLatestByEntityIds(any(), any(), any());
        verify(planQueryService, times(1)).findAllByReferenceIdsAndEnvironments(any(), any(), any());
        assertThat(products).allMatch(product -> product.getDeploymentState() == ApiProduct.DeploymentState.DEPLOYED);
    }

    @Test
    void should_ask_the_store_for_nothing_when_there_are_no_products() {
        service.computeDeploymentState(List.of());

        verify(eventLatestQueryService, times(0)).findLatestByEntityIds(any(), any(), any());
        verify(planQueryService, times(0)).findAllByReferenceIdsAndEnvironments(any(), any(), any());
    }

    @Test
    void should_read_each_product_against_its_own_deploy_event() throws Exception {
        var deployed = aProduct("id-1");
        var drifted = aProduct("id-2");
        drifted.setApiIds(Set.of("api-1", "api-2"));
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(deployed), aMatchingDeployEventFor(aProduct("id-2"))));

        service.computeDeploymentState(List.of(deployed, drifted));

        assertThat(deployed.getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
        assertThat(drifted.getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    private ApiProduct aProduct(String id) {
        return ApiProduct.builder().id(id).name(id).environmentId(ENV_ID).apiIds(Set.of("api-1")).build();
    }

    private Event aMatchingDeployEventFor(ApiProduct product) {
        try {
            return Event.builder()
                .id("event-" + product.getId())
                .type(EventType.DEPLOY_API_PRODUCT)
                .properties(new EnumMap<>(Map.of(Event.EventProperties.API_PRODUCT_ID, product.getId())))
                .payload(objectMapper.writeValueAsString(aProduct(product.getId())))
                .environments(Set.of(ENV_ID))
                .createdAt(DEPLOYED_AT.atZone(ZoneId.systemDefault()))
                .updatedAt(DEPLOYED_AT.atZone(ZoneId.systemDefault()))
                .build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
