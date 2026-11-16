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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.PlanFixtures;
import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeployApiProductDomainServiceTest {

    private static final String PRODUCT_ID = "api-product-id";
    private static final String ENV_ID = "env-id";
    private static final AuditInfo AUDIT_INFO = new AuditInfo("org-id", ENV_ID, AuditActor.builder().userId("user-id").build());

    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final EventCrudInMemory eventCrudService = new EventCrudInMemory();
    private final EventLatestCrudInMemory eventLatestCrudService = new EventLatestCrudInMemory();
    private final EventLatestQueryServiceInMemory eventLatestQueryService = new EventLatestQueryServiceInMemory();
    private final FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    private final ObjectMapper objectMapper = GraviteeJacksonMapper.getInstance();

    private final DeployApiProductDomainService service = new DeployApiProductDomainService(
        planQueryService,
        eventCrudService,
        eventLatestCrudService,
        eventLatestQueryService,
        flowCrudService,
        objectMapper
    );

    @AfterEach
    void tearDown() {
        planQueryService.reset();
        eventCrudService.reset();
        eventLatestCrudService.reset();
        eventLatestQueryService.reset();
        flowCrudService.reset();
    }

    @Test
    void should_deploy_the_product_flows_in_order_its_flow_mode_and_the_flows_of_its_plans() {
        var product = aProduct(Set.of("api-1"), bestMatch());
        flowCrudService.saveApiProductFlows(PRODUCT_ID, List.of(aFlow("first"), aFlow("second")));
        givenAPlanWithFlows("plan-1", aFlow("budget"));

        service.deploy(AUDIT_INFO, product);

        var payload = lastPayload();
        assertThat(names(payload.get("flows"))).containsExactly("first", "second");
        assertThat(payload.at("/flowExecution/mode").asText()).isEqualTo("best-match");
        assertThat(names(payload.at("/plans/0/flows"))).containsExactly("budget");
    }

    @Test
    void should_deploy_the_default_flow_mode_and_no_flows_for_a_product_that_saved_none() {
        service.deploy(AUDIT_INFO, aProduct(Set.of("api-1"), null));

        var payload = lastPayload();
        assertThat(payload.get("flows")).isEmpty();
        assertThat(payload.at("/flowExecution/mode").asText()).isEqualTo("default");
        assertThat(payload.at("/flowExecution/matchRequired").asBoolean()).isFalse();
    }

    @Test
    void should_deploy_only_the_api_list_when_the_apis_of_the_product_change() {
        givenAPlanWithFlows("plan-1", aFlow("deployed-budget"));
        flowCrudService.saveApiProductFlows(PRODUCT_ID, List.of(aFlow("deployed")));
        service.deploy(AUDIT_INFO, aProduct(Set.of("api-1"), null).toBuilder().name("Deployed name").build());
        publishLatestDeploy();
        // Saved since the deploy, not deployed.
        flowCrudService.saveApiProductFlows(PRODUCT_ID, List.of(aFlow("saved")));
        flowCrudService.savePlanFlows("plan-1", List.of(aFlow("saved-budget")));
        var saved = aProduct(Set.of("api-1", "api-2"), bestMatch()).toBuilder().name("Saved name").build();

        service.deployApiList(AUDIT_INFO, saved);

        var payload = lastPayload();
        assertThat(texts(payload.get("apiIds"))).containsExactlyInAnyOrder("api-1", "api-2");
        assertThat(names(payload.get("flows"))).containsExactly("deployed");
        assertThat(payload.at("/flowExecution/mode").asText()).isEqualTo("default");
        assertThat(names(payload.at("/plans/0/flows"))).containsExactly("deployed-budget");
        assertThat(payload.get("name").asText()).isEqualTo("Deployed name");
    }

    @Test
    void should_deploy_the_whole_product_when_its_apis_change_before_any_deploy() {
        flowCrudService.saveApiProductFlows(PRODUCT_ID, List.of(aFlow("saved")));

        service.deployApiList(AUDIT_INFO, aProduct(Set.of("api-1", "api-2"), bestMatch()));

        var payload = lastPayload();
        assertThat(texts(payload.get("apiIds"))).containsExactlyInAnyOrder("api-1", "api-2");
        assertThat(names(payload.get("flows"))).containsExactly("saved");
        assertThat(payload.at("/flowExecution/mode").asText()).isEqualTo("best-match");
    }

    @Test
    void should_deploy_the_resources_of_the_product() {
        service.deploy(AUDIT_INFO, aProduct(Set.of("api-1"), null).toBuilder().resources(List.of(aCacheResource())).build());

        var payload = lastPayload();
        assertThat(payload.at("/resources/0/name").asText()).isEqualTo("cache");
        assertThat(payload.at("/resources/0/type").asText()).isEqualTo("cache");
        assertThat(payload.at("/resources/0/configuration/ttl").asInt()).isEqualTo(10);
        assertThat(payload.at("/resources/0/enabled").asBoolean()).isTrue();
    }

    @Test
    void should_stay_deployed_after_deploying_a_product_with_resources() {
        var product = aProduct(Set.of("api-1"), null).toBuilder().resources(List.of(aCacheResource())).build();
        service.deploy(AUDIT_INFO, product);
        publishLatestDeploy();

        new ApiProductDeploymentStateDomainService(
            eventLatestQueryService,
            planQueryService,
            flowCrudService,
            objectMapper
        ).computeDeploymentState(product);

        assertThat(product.getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
    }

    private void givenAPlanWithFlows(String planId, Flow flow) {
        planQueryService.initWith(
            List.of(
                PlanFixtures.HttpV4.anApiKey()
                    .toBuilder()
                    .id(planId)
                    .referenceId(PRODUCT_ID)
                    .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                    .build()
            )
        );
        flowCrudService.savePlanFlows(planId, List.of(flow));
    }

    /** The in-memory latest-event store and query service do not share storage, as the real ones do. */
    private void publishLatestDeploy() {
        eventLatestQueryService.initWith(
            eventLatestCrudService
                .storage()
                .stream()
                .<Event>map(event -> event.toBuilder().updatedAt(ZonedDateTime.now()).build())
                .toList()
        );
    }

    @SneakyThrows
    private JsonNode lastPayload() {
        var events = eventCrudService.storage();
        Event last = events.getLast();
        assertThat(last.getType()).isEqualTo(EventType.DEPLOY_API_PRODUCT);
        return objectMapper.readTree(last.getPayload());
    }

    private static ApiProduct aProduct(Set<String> apiIds, FlowExecution flowExecution) {
        return ApiProduct.builder()
            .id(PRODUCT_ID)
            .name("Product")
            .environmentId(ENV_ID)
            .apiIds(apiIds)
            .flowExecution(flowExecution)
            .build();
    }

    private static Resource aCacheResource() {
        return Resource.builder().name("cache").type("cache").configuration("{\"ttl\":10}").enabled(true).build();
    }

    private static FlowExecution bestMatch() {
        var flowExecution = new FlowExecution();
        flowExecution.setMode(FlowMode.BEST_MATCH);
        return flowExecution;
    }

    private static Flow aFlow(String name) {
        return Flow.builder().name(name).enabled(true).build();
    }

    private static List<String> names(JsonNode flows) {
        return StreamSupport.stream(flows.spliterator(), false)
            .map(flow -> flow.get("name").asText())
            .toList();
    }

    private static List<String> texts(JsonNode values) {
        return StreamSupport.stream(values.spliterator(), false).map(JsonNode::asText).toList();
    }
}
