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

import static org.assertj.core.api.Assertions.*;

import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import java.sql.Date;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiCRDAdapterTest {

    @Test
    void should_convert_to_crd_spec() {
        var export = exportEntity();
        var spec = ApiCRDAdapter.INSTANCE.toCRDSpec(export, export.getApiEntity());
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
        var export = exportEntity();
        var plansWithOneClosedPlan = new HashSet<>(export.getPlans());
        plansWithOneClosedPlan.add(PlanEntity.builder().status(PlanStatus.CLOSED).name("closed-plan").build());
        export.setPlans(plansWithOneClosedPlan);
        var spec = ApiCRDAdapter.INSTANCE.toCRDSpec(export, export.getApiEntity());
        assertThat(spec.getPlans()).hasSize(1);
    }

    @Test
    void should_resolve_conflicts_with_plan_names_in_crd_spec() {
        var export = exportEntity();
        var then = Instant.now();
        var plansWithConflictingNames = Set.of(
            PlanEntity
                .builder()
                .createdAt(Date.from(then))
                .id("api-key-1")
                .name("api-key")
                .security(PlanSecurity.builder().type("API_KEY").build())
                .build(),
            PlanEntity
                .builder()
                .createdAt(Date.from(then.plusMillis(1)))
                .id("api-key-2")
                .name("api-key")
                .security(PlanSecurity.builder().type("API_KEY").build())
                .build(),
            PlanEntity
                .builder()
                .createdAt(Date.from(then.plusMillis(2)))
                .id("api-key-3")
                .name("api-key")
                .security(PlanSecurity.builder().type("API_KEY").build())
                .build()
        );
        export.setPlans(plansWithConflictingNames);
        var spec = ApiCRDAdapter.INSTANCE.toCRDSpec(export, export.getApiEntity());
        assertThat(spec.getPlans()).hasSize(3);
    }

    private static ExportApiEntity exportEntity() {
        return ExportApiEntity
            .builder()
            .apiEntity(
                ApiEntity
                    .builder()
                    .name("api-name")
                    .id("api-id")
                    .crossId("api-cross-id")
                    .listeners(List.of(HttpListener.builder().paths(List.of(new Path("/api-path"))).build()))
                    .endpointGroups(
                        List.of(
                            EndpointGroup
                                .builder()
                                .name("default-group")
                                .type("http-proxy")
                                .sharedConfiguration("{}")
                                .endpoints(
                                    List.of(
                                        Endpoint
                                            .builder()
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
            .plans(Set.of(PlanEntity.builder().name("plan-name").id("plan-id").security(new PlanSecurity("key-less", "{}")).build()))
            .build();
    }
}
