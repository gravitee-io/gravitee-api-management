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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import fixtures.definition.PlanFixtures;
import inmemory.EventLatestQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductDeploymentPayload;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
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
    /** Like an API's, they change what operators see, not what the gateway runs. */
    private static final Set<String> NOT_COMPARED = Set.of("name", "description", "version");

    private final EventLatestQueryServiceInMemory eventLatestQueryService = spy(new EventLatestQueryServiceInMemory());
    private final PlanQueryServiceInMemory planQueryService = spy(new PlanQueryServiceInMemory());
    private final FlowCrudServiceInMemory flowCrudService = spy(new FlowCrudServiceInMemory());
    private final ObjectMapper objectMapper = GraviteeJacksonMapper.getInstance();

    private final ApiProductDeploymentStateDomainService service = new ApiProductDeploymentStateDomainService(
        eventLatestQueryService,
        planQueryService,
        flowCrudService,
        objectMapper
    );

    @AfterEach
    void tearDown() {
        eventLatestQueryService.reset();
        planQueryService.reset();
        flowCrudService.reset();
    }

    @Test
    void should_cost_the_same_four_queries_however_many_products_there_are() {
        // A query per product would make a page of twenty-five cost a hundred round trips, and a listing's cost
        // grow with the environment.
        var products = IntStream.range(0, 25)
            .mapToObj(i -> aProduct("id-" + i))
            .toList();
        eventLatestQueryService.initWith(products.stream().map(this::aMatchingDeployEventFor).toList());

        service.computeDeploymentState(products);

        verify(eventLatestQueryService, times(1)).findLatestByEntityIds(any(), any(), any());
        verify(planQueryService, times(1)).findAllByReferenceIdsAndEnvironments(any(), any(), any());
        verify(flowCrudService, times(1)).getApiProductFlows(anySet());
        verify(flowCrudService, times(1)).getPlanV4Flows(anySet());
        assertThat(products).allMatch(product -> product.getDeploymentState() == ApiProduct.DeploymentState.DEPLOYED);
    }

    @Test
    void should_ask_the_store_for_nothing_when_there_are_no_products() {
        service.computeDeploymentState(List.of());

        verify(eventLatestQueryService, times(0)).findLatestByEntityIds(any(), any(), any());
        verify(planQueryService, times(0)).findAllByReferenceIdsAndEnvironments(any(), any(), any());
        verify(flowCrudService, times(0)).getApiProductFlows(anySet());
    }

    @Test
    void should_read_each_product_against_its_own_deploy_event() {
        var deployed = aProduct("id-1");
        var drifted = aProduct("id-2");
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(deployed), aMatchingDeployEventFor(aProduct("id-2"))));
        drifted.setApiIds(Set.of("api-1", "api-2"));

        service.computeDeploymentState(List.of(deployed, drifted));

        assertThat(deployed.getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
        assertThat(drifted.getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    @Test
    void should_stay_deployed_when_resources_are_only_reordered() throws Exception {
        Resource cache = Resource.builder().name("cache").type("cache").configuration("{\"ttl\":10}").build();
        Resource oauth = Resource.builder().name("oauth").type("oauth2").configuration("{}").build();
        var product = aProduct("id-1");
        product.setResources(List.of(oauth, cache));
        var deployed = aProduct("id-1");
        deployed.setResources(List.of(cache, oauth));
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(deployed)));

        service.computeDeploymentState(List.of(product));

        assertThat(product.getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
    }

    @Test
    void should_need_redeploy_when_a_resource_is_added() throws Exception {
        var product = aProduct("id-1");
        product.setResources(List.of(Resource.builder().name("cache").type("cache").configuration("{\"ttl\":10}").build()));
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(aProduct("id-1"))));

        service.computeDeploymentState(List.of(product));

        assertThat(product.getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    @Test
    void should_need_redeploy_when_a_resource_is_removed() throws Exception {
        Resource cache = Resource.builder().name("cache").type("cache").configuration("{\"ttl\":10}").build();
        Resource oauth = Resource.builder().name("oauth").type("oauth2").configuration("{}").build();
        var deployed = aProduct("id-1");
        deployed.setResources(List.of(cache, oauth));
        var product = aProduct("id-1");
        product.setResources(List.of(cache));
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(deployed)));

        service.computeDeploymentState(List.of(product));

        assertThat(product.getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    @Test
    void should_stay_deployed_when_resource_configuration_differs_only_in_key_order_and_spacing() throws Exception {
        var deployed = aProduct("id-1");
        deployed.setResources(
            List.of(
                Resource.builder().name("cache").type("cache").configuration("{ \"maxEntries\": 5, \"ttl\": 10 }").enabled(true).build()
            )
        );
        var product = aProduct("id-1");
        product.setResources(
            List.of(Resource.builder().name("cache").type("cache").configuration("{\"ttl\":10,\"maxEntries\":5}").enabled(true).build())
        );
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(deployed)));

        service.computeDeploymentState(List.of(product));

        assertThat(product.getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
    }

    @Test
    void should_need_redeploy_when_a_resource_configuration_value_changes() throws Exception {
        var deployed = aProduct("id-1");
        deployed.setResources(
            List.of(Resource.builder().name("cache").type("cache").configuration("{\"ttl\":10,\"maxEntries\":5}").enabled(true).build())
        );
        var product = aProduct("id-1");
        product.setResources(
            List.of(Resource.builder().name("cache").type("cache").configuration("{\"ttl\":20,\"maxEntries\":5}").enabled(true).build())
        );
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(deployed)));

        service.computeDeploymentState(List.of(product));

        assertThat(product.getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    @Test
    void should_need_redeploy_when_a_resource_configuration_changes() throws Exception {
        var deployed = aProduct("id-1");
        deployed.setResources(List.of(Resource.builder().name("cache").type("cache").configuration("{\"ttl\":10}").enabled(true).build()));
        var product = aProduct("id-1");
        product.setResources(List.of(Resource.builder().name("cache").type("cache").configuration("{\"ttl\":20}").enabled(true).build()));
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(deployed)));

        service.computeDeploymentState(List.of(product));

        assertThat(product.getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    @Test
    void should_need_redeploy_when_a_resource_is_disabled() throws Exception {
        var deployed = aProduct("id-1");
        deployed.setResources(List.of(Resource.builder().name("cache").type("cache").configuration("{\"ttl\":10}").enabled(true).build()));
        var product = aProduct("id-1");
        product.setResources(List.of(Resource.builder().name("cache").type("cache").configuration("{\"ttl\":10}").enabled(false).build()));
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(deployed)));

        service.computeDeploymentState(List.of(product));

        assertThat(product.getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    @Test
    void should_need_redeploy_when_an_empty_list_replaces_no_resources() throws Exception {
        var product = aProduct("id-1");
        product.setResources(List.of());
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(aProduct("id-1"))));

        service.computeDeploymentState(List.of(product));

        assertThat(product.getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    @Test
    void should_keep_a_product_deployed_before_product_flows_existed_deployed() {
        var product = aProduct("id-1");
        product.setApiIds(Set.of("api-1", "api-2"));
        var legacyPayload = Map.of(
            "id",
            "id-1",
            "name",
            "id-1",
            "environmentId",
            ENV_ID,
            "apiIds",
            List.of("api-2", "api-1"),
            "plans",
            List.of()
        );
        eventLatestQueryService.initWith(List.of(aDeployEvent("id-1", legacyPayload)));

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
    }

    @Test
    void should_keep_a_legacy_deploy_deployed_when_the_product_saved_no_flows_in_the_default_mode() {
        var product = aProduct("id-1");
        product.setFlowExecution(new FlowExecution());
        var legacyPayload = Map.of("id", "id-1", "name", "id-1", "environmentId", ENV_ID, "apiIds", List.of("api-1"));
        eventLatestQueryService.initWith(List.of(aDeployEvent("id-1", legacyPayload)));

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
    }

    @Test
    void should_need_a_redeploy_when_the_product_flows_change() {
        var product = aProduct("id-1");
        flowCrudService.saveApiProductFlows("id-1", List.of(aFlow("first")));
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(product)));

        flowCrudService.saveApiProductFlows("id-1", List.of(aFlow("first"), aFlow("second")));

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    @Test
    void should_need_a_redeploy_when_the_product_flows_are_reordered() {
        var product = aProduct("id-1");
        flowCrudService.saveApiProductFlows("id-1", List.of(aFlow("first"), aFlow("second")));
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(product)));

        flowCrudService.saveApiProductFlows("id-1", List.of(aFlow("second"), aFlow("first")));

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    @Test
    void should_need_a_redeploy_when_the_flow_mode_changes() {
        var product = aProduct("id-1");
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(product)));
        var bestMatch = new FlowExecution();
        bestMatch.setMode(FlowMode.BEST_MATCH);

        product.setFlowExecution(bestMatch);

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    @Test
    void should_need_a_redeploy_when_the_analytics_change() {
        var product = aProduct("id-1");
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(product)));

        product.setAnalytics(Analytics.builder().enabled(true).build());

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    @Test
    void should_stay_deployed_right_after_deploying_flows_whose_steps_have_a_configuration() {
        var product = aProduct("id-1");
        var configured = Step.builder().policy("rate-limit").configuration("{\"rate\":{\"limit\":10}}").build();
        flowCrudService.saveApiProductFlows("id-1", List.of(aFlow("first").toBuilder().request(List.of(configured)).build()));
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(product)));

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
    }

    @Test
    void should_stay_deployed_when_the_plans_are_as_deployed_in_another_order() {
        var product = aProduct("id-1");
        var first = aPlan("plan-1", PlanStatus.PUBLISHED);
        var second = aPlan("plan-2", PlanStatus.DEPRECATED);
        flowCrudService.savePlanFlows("plan-1", List.of(aFlow("plan flow")));
        planQueryService.initWith(List.of(first, second));
        eventLatestQueryService.initWith(List.of(aDeployEvent("id-1", aPayloadWithPlans(product, List.of(second, first)))));

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
    }

    @Test
    void should_stay_deployed_when_the_deployed_plan_tags_are_in_another_order() {
        var product = aProduct("id-1");
        var deployed = aPlan("plan-1", PlanStatus.PUBLISHED);
        deployed.getPlanDefinitionHttpV4().setTags(new LinkedHashSet<>(List.of("a", "b")));
        var plan = aPlan("plan-1", PlanStatus.PUBLISHED);
        plan.getPlanDefinitionHttpV4().setTags(new LinkedHashSet<>(List.of("b", "a")));
        planQueryService.initWith(List.of(plan));
        eventLatestQueryService.initWith(List.of(aDeployEvent("id-1", aPayloadWithPlans(product, List.of(deployed)))));

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
    }

    @Test
    void should_need_a_redeploy_when_a_plan_differs_from_the_deployed_one_whenever_it_was_saved() {
        // An API list change deploys the previous plans in a newer event, so a plan saved before that event is
        // still not live: only the payload tells.
        var product = aProduct("id-1");
        var plan = aPlan("plan-1", PlanStatus.PUBLISHED);
        planQueryService.initWith(List.of(plan));
        eventLatestQueryService.initWith(List.of(aDeployEvent("id-1", aPayloadWithPlans(product, List.of(plan)))));

        flowCrudService.savePlanFlows("plan-1", List.of(aFlow("saved before the last deploy event")));

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    @Test
    void should_need_a_redeploy_when_a_deployed_plan_was_closed() {
        var product = aProduct("id-1");
        var deployed = aPlan("plan-1", PlanStatus.PUBLISHED);
        eventLatestQueryService.initWith(List.of(aDeployEvent("id-1", aPayloadWithPlans(product, List.of(deployed)))));

        planQueryService.initWith(List.of(aPlan("plan-1", PlanStatus.CLOSED)));

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    @Test
    void should_stay_deployed_when_only_the_name_description_or_version_changed() {
        var product = aFullProduct();
        eventLatestQueryService.initWith(List.of(aMatchingDeployEventFor(product)));

        product.setName("renamed");
        product.setDescription("described again");
        product.setVersion("2.0");

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
    }

    @Test
    void should_stay_deployed_when_only_a_plan_was_renamed() {
        var product = aProduct("id-1");
        var plan = aPlan("plan-1", PlanStatus.PUBLISHED);
        eventLatestQueryService.initWith(List.of(aDeployEvent("id-1", aPayloadWithPlans(product, List.of(plan)))));

        var renamed = aPlan("plan-1", PlanStatus.PUBLISHED);
        renamed.setName("renamed");
        renamed.getPlanDefinitionHttpV4().setName("renamed");
        planQueryService.initWith(List.of(renamed));

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
    }

    @Test
    void should_stay_deployed_when_only_a_staging_plan_changed() {
        var product = aProduct("id-1");
        eventLatestQueryService.initWith(List.of(aDeployEvent("id-1", aPayloadWithPlans(product, List.of()))));

        planQueryService.initWith(List.of(aPlan("plan-1", PlanStatus.STAGING)));

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
    }

    @Test
    void should_stay_deployed_when_only_the_order_of_the_apis_and_tags_differs() {
        var product = aProduct("id-1");
        product.setApiIds(new LinkedHashSet<>(List.of("api-1", "api-2", "api-3")));
        product.setTags(new LinkedHashSet<>(List.of("internal", "external")));
        var deployed = (ObjectNode) objectMapper.valueToTree(ApiProductDeploymentPayload.of(product, List.of(), List.of()));
        deployed.set("apiIds", objectMapper.valueToTree(List.of("api-3", "api-1", "api-2")));
        deployed.set("tags", objectMapper.valueToTree(List.of("external", "internal")));
        eventLatestQueryService.initWith(List.of(aDeployEvent("id-1", deployed)));

        assertThat(service.computeDeploymentState(product).getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
    }

    @Test
    void should_compare_every_field_of_the_payload() {
        var product = aFullProduct();
        flowCrudService.saveApiProductFlows(product.getId(), List.of(aFlow("first")));
        var plan = aPlan("plan-1", PlanStatus.PUBLISHED);
        planQueryService.initWith(List.of(plan));
        var fields = Arrays.stream(ApiProductDeploymentPayload.class.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .map(Field::getName)
            .filter(name -> !NOT_COMPARED.contains(name))
            .toList();
        assertThat(fields).contains("plans", "flows", "flowExecution", "analytics");

        for (String field : fields) {
            var deployed = (ObjectNode) objectMapper.valueToTree(aPayloadWithPlans(product, List.of(plan)));
            deployed.set(field, TextNode.valueOf("changed since the deploy"));
            eventLatestQueryService.reset();
            eventLatestQueryService.initWith(List.of(aDeployEvent(product.getId(), deployed)));

            assertThat(service.computeDeploymentState(product).getDeploymentState())
                .as("a change to '%s' since the deploy", field)
                .isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
        }
    }

    private ApiProduct aProduct(String id) {
        return ApiProduct.builder().id(id).name(id).environmentId(ENV_ID).apiIds(Set.of("api-1")).build();
    }

    private ApiProduct aFullProduct() {
        var bestMatch = new FlowExecution();
        bestMatch.setMode(FlowMode.BEST_MATCH);
        return aProduct("id-1")
            .toBuilder()
            .description("description")
            .version("1.0")
            .tags(Set.of("internal"))
            .analytics(Analytics.builder().enabled(true).build())
            .flowExecution(bestMatch)
            .build();
    }

    private static Flow aFlow(String name) {
        return Flow.builder().name(name).enabled(true).build();
    }

    private static Plan aPlan(String id, PlanStatus status) {
        return Plan.builder()
            .id(id)
            .environmentId(ENV_ID)
            .referenceId("id-1")
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .definitionVersion(DefinitionVersion.V4)
            .apiType(ApiType.PROXY)
            .planDefinitionHttpV4(
                PlanFixtures.HttpV4Definition.anApiKeyV4().toBuilder().id(id).status(status).tags(Set.of("b", "a")).build()
            )
            .build();
    }

    /** What a deploy would have written while the given plans were as they are now, with their flows as stored now. */
    private ApiProductDeploymentPayload aPayloadWithPlans(ApiProduct product, List<Plan> plans) {
        var definitions = plans
            .stream()
            .map(plan -> {
                var definition = plan.getPlanDefinitionHttpV4();
                definition.setFlows(flowCrudService.getPlanV4Flows(plan.getId()));
                return definition;
            })
            .toList();
        return ApiProductDeploymentPayload.of(product, definitions, flowCrudService.getApiProductFlows(product.getId()));
    }

    /** What a deploy of the product, as it is now, would have written. */
    private Event aMatchingDeployEventFor(ApiProduct product) {
        return aDeployEvent(
            product.getId(),
            ApiProductDeploymentPayload.of(product, List.of(), flowCrudService.getApiProductFlows(product.getId()))
        );
    }

    private Event aDeployEvent(String productId, Object payload) {
        try {
            return Event.builder()
                .id("event-" + productId)
                .type(EventType.DEPLOY_API_PRODUCT)
                .properties(new EnumMap<>(Map.of(Event.EventProperties.API_PRODUCT_ID, productId)))
                .payload(objectMapper.writeValueAsString(payload))
                .environments(Set.of(ENV_ID))
                .createdAt(DEPLOYED_AT.atZone(ZoneId.systemDefault()))
                .updatedAt(DEPLOYED_AT.atZone(ZoneId.systemDefault()))
                .build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
