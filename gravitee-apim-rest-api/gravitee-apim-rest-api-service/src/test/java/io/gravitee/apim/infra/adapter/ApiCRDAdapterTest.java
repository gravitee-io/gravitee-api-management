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
package io.gravitee.apim.infra.adapter;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.*;

import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageSourceEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiCRDAdapterTest {

    @Test
    void should_convert_to_crd_spec_with_hrid() {
        var export = exportEntity(true);
        var spec = ApiCRDAdapter.INSTANCE.toCRDSpec(export, export.getApiEntity(), null);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(spec.getId()).isEqualTo("api-id");
            soft.assertThat(spec.getName()).isEqualTo("api-name");
            soft.assertThat(spec.getHrid()).isEqualTo("api-hrid");
            soft.assertThat(spec.getCrossId()).isEqualTo("api-cross-id");
            soft.assertThat(spec.getListeners()).hasSize(1);
            soft.assertThat(spec.getEndpointGroups()).hasSize(1);
            soft.assertThat(spec.getPlans()).hasSize(1);
            soft.assertThat(spec.getPlans()).containsKey("plan-hrid");
        });
    }

    @Test
    void should_convert_to_crd_spec_without_hrid() {
        var export = exportEntity(false);
        var spec = ApiCRDAdapter.INSTANCE.toCRDSpec(export, export.getApiEntity(), null);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(spec.getId()).isEqualTo("api-id");
            soft.assertThat(spec.getName()).isEqualTo("api-name");
            soft.assertThat(spec.getHrid()).isNull();
            soft.assertThat(spec.getCrossId()).isEqualTo("api-cross-id");
            soft.assertThat(spec.getListeners()).hasSize(1);
            soft.assertThat(spec.getEndpointGroups()).hasSize(1);
            soft.assertThat(spec.getPlans()).hasSize(1);
            soft.assertThat(spec.getPlans()).containsKey("plan-name");
        });
    }

    @Test
    void should_convert_nativeApi_to_crd_spec_with_hrid() {
        var export = exportNativeApiEntity(true);
        var spec = ApiCRDAdapter.INSTANCE.toCRDSpec(export, export.getApiEntity(), null);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(spec.getId()).isEqualTo("api-id");
            soft.assertThat(spec.getName()).isEqualTo("api-name");
            soft.assertThat(spec.getCrossId()).isEqualTo("api-cross-id");
            soft.assertThat(spec.getListeners()).hasSize(1);
            soft.assertThat(spec.getEndpointGroups()).hasSize(1);
            soft.assertThat(spec.getPlans()).hasSize(1);
            soft.assertThat(spec.getPlans()).containsKey("plan-hrid");
        });
    }

    @Test
    void should_remove_page_content() {
        var export = exportEntity(false);
        PageSourceEntity source = new PageSourceEntity();
        source.setType("http-fetcher");
        export.setPages(
            List.of(PageEntity.builder().name("page-name").type(PageType.SWAGGER.name()).content("content").source(source).build())
        );
        var spec = ApiCRDAdapter.INSTANCE.toCRDSpec(export, export.getApiEntity(), null);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(spec.getId()).isEqualTo("api-id");
            soft.assertThat(spec.getName()).isEqualTo("api-name");
            soft.assertThat(spec.getCrossId()).isEqualTo("api-cross-id");
            soft.assertThat(spec.getListeners()).hasSize(1);
            soft.assertThat(spec.getEndpointGroups()).hasSize(1);
            soft.assertThat(spec.getPlans()).hasSize(1);
            soft.assertThat(spec.getPlans()).containsKey("plan-name");
            soft.assertThat(spec.getPages()).containsKey("page-name");
            soft.assertThat(spec.getPages().get("page-name").getContent()).isNull();
        });
    }

    @Test
    void should_remove_page() {
        var export = exportEntity(false);
        PageSourceEntity source = new PageSourceEntity();
        source.setType("github-fetcher");
        export.setPages(
            List.of(
                PageEntity.builder()
                    .metadata(Map.of("graviteeio/fetcher_type", "auto_fetched"))
                    .name("page-name")
                    .type(PageType.SWAGGER.name())
                    .content("content")
                    .source(source)
                    .build()
            )
        );
        var spec = ApiCRDAdapter.INSTANCE.toCRDSpec(export, export.getApiEntity(), null);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(spec.getId()).isEqualTo("api-id");
            soft.assertThat(spec.getName()).isEqualTo("api-name");
            soft.assertThat(spec.getCrossId()).isEqualTo("api-cross-id");
            soft.assertThat(spec.getListeners()).hasSize(1);
            soft.assertThat(spec.getEndpointGroups()).hasSize(1);
            soft.assertThat(spec.getPlans()).hasSize(1);
            soft.assertThat(spec.getPlans()).containsKey("plan-name");
            soft.assertThat(spec.getPages()).doesNotContainKey("page-name");
        });
    }

    @Test
    void should_remove_folder() {
        var export = exportEntity(false);
        PageSourceEntity source = new PageSourceEntity();
        source.setType("github-fetcher");
        export.setPages(List.of(PageEntity.builder().name("page-name").type(PageType.FOLDER.name()).source(source).build()));
        var spec = ApiCRDAdapter.INSTANCE.toCRDSpec(export, export.getApiEntity(), null);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(spec.getId()).isEqualTo("api-id");
            soft.assertThat(spec.getName()).isEqualTo("api-name");
            soft.assertThat(spec.getCrossId()).isEqualTo("api-cross-id");
            soft.assertThat(spec.getListeners()).hasSize(1);
            soft.assertThat(spec.getEndpointGroups()).hasSize(1);
            soft.assertThat(spec.getPlans()).hasSize(1);
            soft.assertThat(spec.getPlans()).containsKey("plan-name");
            soft.assertThat(spec.getPages()).doesNotContainKey("page-name");
        });
    }

    @Test
    void should_remove_page_content_nativeApi() {
        var export = exportNativeApiEntity(false);
        PageSourceEntity source = new PageSourceEntity();
        source.setType("github-fetcher");
        export.setPages(
            List.of(PageEntity.builder().name("page-name").type(PageType.SWAGGER.name()).content("content").source(source).build())
        );
        var spec = ApiCRDAdapter.INSTANCE.toCRDSpec(export, export.getApiEntity(), null);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(spec.getId()).isEqualTo("api-id");
            soft.assertThat(spec.getName()).isEqualTo("api-name");
            soft.assertThat(spec.getCrossId()).isEqualTo("api-cross-id");
            soft.assertThat(spec.getListeners()).hasSize(1);
            soft.assertThat(spec.getEndpointGroups()).hasSize(1);
            soft.assertThat(spec.getPlans()).hasSize(1);
            soft.assertThat(spec.getPlans()).containsKey("plan-name");
            soft.assertThat(spec.getPages()).containsKey("page-name");
            soft.assertThat(spec.getPages().get("page-name").getContent()).isNull();
        });
    }

    @Test
    void should_convert_nativeApi_to_crd_spec_without_hrid() {
        var export = exportNativeApiEntity(false);
        var spec = ApiCRDAdapter.INSTANCE.toCRDSpec(export, export.getApiEntity(), null);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(spec.getId()).isEqualTo("api-id");
            soft.assertThat(spec.getName()).isEqualTo("api-name");
            soft.assertThat(spec.getCrossId()).isEqualTo("api-cross-id");
            soft.assertThat(spec.getListeners()).hasSize(1);
            soft.assertThat(spec.getEndpointGroups()).hasSize(1);
            soft.assertThat(spec.getPlans()).hasSize(1);
            soft.assertThat(spec.getPlans()).containsKey("plan-name");
        });
    }

    @Test
    void should_exclude_closed_plans_from_crd_spec() {
        var export = exportEntity(true);
        Set<GenericPlanEntity> plansWithOneClosedPlan = new HashSet<>(export.getPlans());
        plansWithOneClosedPlan.add(PlanEntity.builder().status(PlanStatus.CLOSED).name("closed-plan").build());
        export.setPlans(plansWithOneClosedPlan);
        var spec = ApiCRDAdapter.INSTANCE.toCRDSpec(export, export.getApiEntity(), null);
        assertThat(spec.getPlans()).hasSize(1);
    }

    @Test
    void should_resolve_conflicts_with_plan_names_in_crd_spec() {
        var hridPlan = plan("api-123-plan-1", "plan-1", "api key");
        var firstPlan = plan("api-123-plan-2", null, "api key");
        var secondPlan = plan("api-123-plan-3", null, "api key");

        var plans = toCRDSpec(secondPlan, hridPlan, firstPlan).getPlans();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(keysById(plans)).containsOnlyKeys("api-123-plan-1", "api-123-plan-2", "api-123-plan-3");
            soft.assertThat(keysById(plans)).containsEntry("api-123-plan-1", "plan-1").containsEntry("api-123-plan-2", "api-key");
            soft.assertThat(keysById(plans).get("api-123-plan-3")).matches("api-key-\\d{8}");
        });
    }

    @Test
    void should_give_each_plan_the_same_key_whatever_the_order_of_the_plans() {
        var hridPlan = plan("plan-a", "jwt", "JWT (deprecated)");
        var namePlan = plan("plan-b", null, "jwt");

        var oneOrder = keysById(toCRDSpec(hridPlan, namePlan).getPlans());
        var otherOrder = keysById(toCRDSpec(namePlan, hridPlan).getPlans());

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(oneOrder).containsEntry("plan-a", "jwt");
            soft.assertThat(otherOrder).isEqualTo(oneOrder);
        });
    }

    @ParameterizedTest
    @CsvSource(
        textBlock = """
        ExamplePlan,  ExamplePlan
        Example Plan, Example-Plan
        API Key,      API-Key
        '  JWT  ',    JWT
        Gold 2.0,     Gold-2-0
        plan.v1,      plan-v1
        """
    )
    void should_keep_the_name_based_plan_key_and_its_case(String planName, String expectedKey) {
        var spec = toCRDSpec(plan("plan-id", null, planName));

        assertThat(spec.getPlans()).containsOnlyKeys(expectedKey);
    }

    @ParameterizedTest
    @CsvSource(
        textBlock = """
        Default Keyless (UNSECURED), default-keyless-unsecured
        Café Plan,                   cafe-plan
        """
    )
    void should_slugify_the_plan_key_when_the_name_based_key_is_not_an_hrid(String planName, String expectedKey) {
        var spec = toCRDSpec(plan("plan-id", null, planName));

        assertThat(spec.getPlans()).containsOnlyKeys(expectedKey);
    }

    @ParameterizedTest
    @CsvSource(
        textBlock = """
        ExamplePlan,                          Example Plan v2, ExamplePlan
        exampleplan,                          ExamplePlan,     exampleplan
        API Key,                              Gold,            API-Key
        3f1c2a9e-7b4d-4c1e-9a2f-5d6e7f8a9b0c, ExamplePlan,     3f1c2a9e-7b4d-4c1e-9a2f-5d6e7f8a9b0c
        """
    )
    void should_key_a_plan_by_its_hrid(String planHrid, String planName, String expectedKey) {
        var spec = toCRDSpec(plan("plan-id", planHrid, planName));

        assertThat(spec.getPlans()).containsOnlyKeys(expectedKey);
    }

    @Test
    void should_key_a_plan_by_its_name_when_its_hrid_is_the_plan_id_the_upgrader_copied() {
        var planId = "3f1c2a9e-7b4d-4c1e-9a2f-5d6e7f8a9b0c";

        var spec = toCRDSpec(plan(planId, planId, "ExamplePlan"));

        assertThat(spec.getPlans()).containsOnlyKeys("ExamplePlan");
    }

    @Test
    void should_key_a_plan_by_its_name_when_its_hrid_cannot_be_an_hrid() {
        var spec = toCRDSpec(plan("plan-id", "Example Plan (v2)", "ExamplePlan"));

        assertThat(spec.getPlans()).containsOnlyKeys("ExamplePlan");
    }

    @ParameterizedTest
    @ValueSource(strings = { "ExamplePlan", "Example Plan", "Gold 2.0", "plan.v1", "Default Keyless (UNSECURED)", "Café Plan" })
    void should_export_the_same_key_once_gko_stored_it_as_the_plan_hrid(String planName) {
        var exportedKey = singleKey(toCRDSpec(plan("plan-id", null, planName)));

        var reExportedKey = singleKey(toCRDSpec(plan("plan-id", gkoHrid(exportedKey), planName)));

        assertThat(reExportedKey).isEqualTo(exportedKey);
    }

    @ParameterizedTest
    @ValueSource(strings = { "(!)", "A" })
    void should_key_a_plan_by_an_id_based_suffix_when_its_name_cannot_be_an_hrid(String planName) {
        var key = singleKey(toCRDSpec(plan("plan-id", null, planName)));

        assertThat(key).matches("plan-\\d{8}").isEqualTo(singleKey(toCRDSpec(plan("plan-id", null, planName))));
    }

    /**
     * Mirrors GKO's NameToValidHRID, which turns the spaces and dots of a plan key into dashes to build the plan HRID.
     */
    private static String gkoHrid(String key) {
        return key.replace(' ', '-').replace('.', '-');
    }

    private static GenericPlanEntity plan(String id, String hrid, String name) {
        return PlanEntity.builder().id(id).hrid(hrid).name(name).security(new PlanSecurity("key-less", "{}")).build();
    }

    private static ApiCRDSpec toCRDSpec(GenericPlanEntity... plans) {
        var export = exportEntity(false);
        export.setPlans(new LinkedHashSet<>(List.of(plans)));
        return ApiCRDAdapter.INSTANCE.toCRDSpec(export, export.getApiEntity(), null);
    }

    private static Map<String, String> keysById(Map<String, PlanCRD> plans) {
        return plans.entrySet().stream().collect(toMap(entry -> entry.getValue().getId(), Map.Entry::getKey));
    }

    private static String singleKey(ApiCRDSpec spec) {
        assertThat(spec.getPlans()).hasSize(1);
        return spec.getPlans().keySet().iterator().next();
    }

    private static ExportApiEntity exportEntity(boolean withHrid) {
        return ExportApiEntity.builder()
            .apiEntity(
                ApiEntity.builder()
                    .name("api-name")
                    .id("api-id")
                    .crossId("api-cross-id")
                    .hrid(withHrid ? "api-hrid" : null)
                    .listeners(List.of(HttpListener.builder().paths(List.of(new Path("/api-path"))).build()))
                    .endpointGroups(
                        List.of(
                            EndpointGroup.builder()
                                .name("default-group")
                                .type("http-proxy")
                                .sharedConfiguration("{}")
                                .endpoints(
                                    List.of(
                                        Endpoint.builder()
                                            .name("default-endpoint")
                                            .type("http-proxy")
                                            .inheritConfiguration(true)
                                            .configuration("{\"target\":\"https://api.gravitee.io/echo\"}")
                                            .build()
                                    )
                                )
                                .build()
                        )
                    )
                    .build()
            )
            .plans(
                Set.of(
                    PlanEntity.builder()
                        .hrid(withHrid ? "plan-hrid" : null)
                        .name("plan-name")
                        .id("plan-id")
                        .security(new PlanSecurity("key-less", "{}"))
                        .build()
                )
            )
            .build();
    }

    private static ExportApiEntity exportNativeApiEntity(boolean withHrid) {
        return ExportApiEntity.builder()
            .apiEntity(
                NativeApiEntity.builder()
                    .name("api-name")
                    .id("api-id")
                    .crossId("api-cross-id")
                    .hrid(withHrid ? "api-hrid" : null)
                    .listeners(List.of(KafkaListener.builder().host("myapi").build()))
                    .endpointGroups(
                        List.of(
                            NativeEndpointGroup.builder()
                                .name("default-group")
                                .type("kafka")
                                .sharedConfiguration("{}")
                                .endpoints(
                                    List.of(
                                        NativeEndpoint.builder()
                                            .name("default-endpoint")
                                            .type("kafka")
                                            .inheritConfiguration(true)
                                            .configuration("{\"bootstrapServers\":\"localhost:9092\"}")
                                            .build()
                                    )
                                )
                                .build()
                        )
                    )
                    .build()
            )
            .plans(
                Set.of(
                    NativePlanEntity.builder()
                        .hrid(withHrid ? "plan-hrid" : null)
                        .name("plan-name")
                        .id("plan-id")
                        .security(new PlanSecurity("key-less", "{}"))
                        .build()
                )
            )
            .build();
    }
}
