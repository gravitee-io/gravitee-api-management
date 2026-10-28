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

import inmemory.AbstractUseCaseTest;
import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.definition.model.v4.resource.Resource;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeployApiProductDomainServiceTest extends AbstractUseCaseTest {

    private final EventCrudInMemory eventCrudService = new EventCrudInMemory();
    private final DeployApiProductDomainService service = new DeployApiProductDomainService(
        new PlanQueryServiceInMemory(),
        eventCrudService,
        new EventLatestCrudInMemory(),
        new FlowCrudServiceInMemory()
    );

    @Test
    void should_write_resources_into_the_deploy_event_payload() {
        Resource cache = Resource.builder().name("cache").type("cache").configuration("{\"ttl\":10}").enabled(true).build();
        ApiProduct product = ApiProduct.builder()
            .id("api-product-id")
            .name("API Product")
            .environmentId(ENV_ID)
            .resources(List.of(cache))
            .build();

        service.deploy(AUDIT_INFO, product);

        assertThat(eventCrudService.storage())
            .singleElement()
            .satisfies(event -> {
                assertThat(event.getPayload()).contains("\"name\":\"cache\"");
                assertThat(event.getPayload()).contains("\"type\":\"cache\"");
                assertThat(event.getPayload()).contains("\"ttl\":10");
                assertThat(event.getPayload()).contains("\"enabled\":true");
            });
    }

    @Test
    void should_stay_deployed_after_a_deploy_that_already_contains_the_resources() {
        Resource cache = Resource.builder().name("cache").type("cache").configuration("{\"ttl\":10}").enabled(true).build();
        ApiProduct product = ApiProduct.builder()
            .id("api-product-id")
            .name("API Product")
            .environmentId(ENV_ID)
            .resources(List.of(cache))
            .build();

        service.deploy(AUDIT_INFO, product);
        var deployed = eventCrudService.storage().get(0);
        deployed.setUpdatedAt(ZonedDateTime.now());
        var latestEvents = new EventLatestQueryServiceInMemory();
        latestEvents.initWith(List.of(deployed));

        new ApiProductDeploymentStateDomainService(
            latestEvents,
            new PlanQueryServiceInMemory(),
            GraviteeJacksonMapper.getInstance()
        ).computeDeploymentState(product);

        assertThat(product.getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
    }
}
