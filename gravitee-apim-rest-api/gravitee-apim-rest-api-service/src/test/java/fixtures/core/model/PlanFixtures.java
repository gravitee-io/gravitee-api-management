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
package fixtures.core.model;

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class PlanFixtures {

    private PlanFixtures() {}

    private static final Supplier<Plan.PlanBuilder> BASE = () ->
        Plan
            .builder()
            .id("my-plan")
            .apiId("my-api")
            .name("My plan")
            .description("Description")
            .order(1)
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .crossId("my-plan-crossId")
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO);

    public static Plan aPlanV4() {
        return BASE.get().definitionVersion(DefinitionVersion.V4).planDefinitionV4(fixtures.definition.PlanFixtures.aKeylessV4()).build();
    }

    public static Plan aPlanV2() {
        return BASE.get().definitionVersion(DefinitionVersion.V2).planDefinitionV2(fixtures.definition.PlanFixtures.aKeylessV2()).build();
    }

    public static Plan aKeylessV4() {
        return BASE.get().id("keyless").name("Keyless").planDefinitionV4(fixtures.definition.PlanFixtures.aKeylessV4()).build();
    }

    public static Plan anApiKeyV4() {
        return BASE.get().id("apikey").name("API Key").planDefinitionV4(fixtures.definition.PlanFixtures.anApiKeyV4()).build();
    }

    public static Plan aPushPlan() {
        return BASE.get().id("push").name("Push Plan").planDefinitionV4(fixtures.definition.PlanFixtures.aPushPlan()).build();
    }

    public static Plan anMtlsPlanV4() {
        return BASE
            .get()
            .id("mtls")
            .name("mTLS Plan")
            .definitionVersion(DefinitionVersion.V4)
            .planDefinitionV4(fixtures.definition.PlanFixtures.anMtlsPlanV4())
            .build();
    }

    public static Plan aFederatedPlan() {
        return BASE
            .get()
            .id("federated")
            .name("Federated Plan")
            .federatedPlanDefinition(fixtures.definition.PlanFixtures.aFederatedPlan())
            .validation(Plan.PlanValidationType.MANUAL)
            .build();
    }
}
