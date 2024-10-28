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
package fixtures;

import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class PlanModelFixtures {

    private PlanModelFixtures() {}

    private static final PlanSecurity.PlanSecurityBuilder BASE_PLAN_SECURITY_V4 = PlanSecurity
        .builder()
        .type(PlanSecurityType.API_KEY.getLabel())
        .configuration("{\"nice\": \"config\"}");

    private static final PlanEntity.PlanEntityBuilder<?, ?> BASE_PLAN_ENTITY_V4 = PlanEntity
        .builder()
        .id("my-plan")
        .apiId("my-api")
        .name("My plan")
        .description("Description")
        .order(1)
        .characteristics(List.of("characteristic1", "characteristic2"))
        .createdAt(new Date())
        .updatedAt(new Date())
        .commentMessage("Comment message")
        .crossId("my-plan-crossId")
        .generalConditions("General conditions")
        .tags(Set.of("tag1", "tag2"))
        .status(PlanStatus.PUBLISHED)
        .security(BASE_PLAN_SECURITY_V4.build())
        .type(PlanType.API)
        .mode(PlanMode.STANDARD)
        .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
        .validation(PlanValidationType.AUTO)
        .selectionRule("{#request.attribute['selectionRule'] != null}")
        .flows(List.of(FlowModelFixtures.aModelFlowHttpV4()));

    private static final io.gravitee.rest.api.model.PlanEntity.PlanEntityBuilder<?, ?> BASE_PLAN_ENTITY_V2 =
        io.gravitee.rest.api.model.PlanEntity
            .builder()
            .id("my-plan")
            .api("my-api")
            .name("My plan")
            .description("Description")
            .order(1)
            .characteristics(List.of("characteristic1", "characteristic2"))
            .createdAt(new Date())
            .updatedAt(new Date())
            .commentMessage("Comment message")
            .crossId("my-plan-crossId")
            .generalConditions("General conditions")
            .tags(Set.of("tag1", "tag2"))
            .status(io.gravitee.rest.api.model.PlanStatus.PUBLISHED)
            .security(io.gravitee.rest.api.model.PlanSecurityType.API_KEY)
            .type(io.gravitee.rest.api.model.PlanType.API)
            .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
            .validation(io.gravitee.rest.api.model.PlanValidationType.AUTO)
            .selectionRule("{#request.attribute['selectionRule'] != null}")
            .flows(List.of(FlowModelFixtures.aModelFlowV2()));

    public static PlanEntity aPlanEntityV4() {
        return BASE_PLAN_ENTITY_V4.build();
    }

    public static PlanEntity aKeylessPlanV4() {
        return PlanEntity
            .builder()
            .id("keyless-id")
            .name("keyless")
            .apiId("my-api")
            .createdAt(Date.from(Instant.parse("2020-01-01T00:00:00.00Z")))
            .updatedAt(Date.from(Instant.parse("2020-02-02T10:15:30.00Z")))
            .status(PlanStatus.PUBLISHED)
            .security(PlanSecurity.builder().type(PlanSecurityType.KEY_LESS.getLabel()).build())
            .type(PlanType.API)
            .validation(PlanValidationType.AUTO)
            .build();
    }

    public static PlanEntity anApiKeyPanV4() {
        return aPlanEntityV4()
            .toBuilder()
            .name("apikey")
            .security(PlanSecurity.builder().type(PlanSecurityType.API_KEY.getLabel()).build())
            .build();
    }

    public static io.gravitee.rest.api.model.PlanEntity aPlanEntityV2() {
        return BASE_PLAN_ENTITY_V2.build();
    }

    public static PlanSecurity aPlanSecurityV4() {
        return BASE_PLAN_SECURITY_V4.build();
    }
}
