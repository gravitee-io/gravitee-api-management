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
import io.gravitee.definition.model.v4.ApiType;
import java.time.Instant;
import java.time.ZoneId;
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

    public static Plan aPlanHttpV4() {
        return BASE
            .get()
            .definitionVersion(DefinitionVersion.V4)
            .apiType(ApiType.PROXY)
            .planDefinitionHttpV4(fixtures.definition.PlanFixtures.HttpV4Definition.aKeylessV4())
            .build();
    }

    public static Plan aPlanNativeV4() {
        return BASE
            .get()
            .definitionVersion(DefinitionVersion.V4)
            .apiType(ApiType.NATIVE)
            .planDefinitionNativeV4(fixtures.definition.PlanFixtures.NativeV4Definition.aKeylessV4())
            .build();
    }

    public static Plan aPlanV2() {
        return BASE.get().definitionVersion(DefinitionVersion.V2).planDefinitionV2(fixtures.definition.PlanFixtures.aKeylessV2()).build();
    }

    public static class HttpV4 {

        private HttpV4() {}

        public static Plan aKeyless() {
            return BASE
                .get()
                .id("keyless")
                .name("Keyless")
                .planDefinitionHttpV4(fixtures.definition.PlanFixtures.HttpV4Definition.aKeylessV4())
                .build();
        }

        public static Plan anApiKey() {
            return BASE
                .get()
                .id("apikey")
                .name("API Key")
                .planDefinitionHttpV4(fixtures.definition.PlanFixtures.HttpV4Definition.anApiKeyV4())
                .build();
        }

        public static Plan aPushPlan() {
            return BASE
                .get()
                .id("push")
                .name("Push Plan")
                .planDefinitionHttpV4(fixtures.definition.PlanFixtures.HttpV4Definition.aPushPlan())
                .build();
        }

        public static Plan anMtlsPlan() {
            return BASE
                .get()
                .id("mtls")
                .name("mTLS Plan")
                .definitionVersion(DefinitionVersion.V4)
                .planDefinitionHttpV4(fixtures.definition.PlanFixtures.HttpV4Definition.anMtlsPlanV4())
                .build();
        }
    }

    public static class NativeV4 {

        private NativeV4() {}

        public static Plan aKeyless() {
            return BASE
                .get()
                .id("keyless")
                .name("Keyless")
                .planDefinitionNativeV4(fixtures.definition.PlanFixtures.NativeV4Definition.aKeylessV4())
                .definitionVersion(DefinitionVersion.V4)
                .apiType(ApiType.NATIVE)
                .build();
        }

        public static Plan anApiKey() {
            return BASE
                .get()
                .id("apikey")
                .name("API Key")
                .planDefinitionNativeV4(fixtures.definition.PlanFixtures.NativeV4Definition.anApiKeyV4())
                .definitionVersion(DefinitionVersion.V4)
                .apiType(ApiType.NATIVE)
                .build();
        }

        public static Plan aPushPlan() {
            return BASE
                .get()
                .id("push")
                .name("Push Plan")
                .planDefinitionNativeV4(fixtures.definition.PlanFixtures.NativeV4Definition.aPushPlan())
                .definitionVersion(DefinitionVersion.V4)
                .apiType(ApiType.NATIVE)
                .build();
        }

        public static Plan anMtlsPlan() {
            return BASE
                .get()
                .id("mtls")
                .name("mTLS Plan")
                .definitionVersion(DefinitionVersion.V4)
                .planDefinitionNativeV4(fixtures.definition.PlanFixtures.NativeV4Definition.anMtlsPlanV4())
                .definitionVersion(DefinitionVersion.V4)
                .apiType(ApiType.NATIVE)
                .build();
        }
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
