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
package io.gravitee.apim.core.plan.model.factory;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.federation.FederatedPlan;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.common.UuidString;

public class PlanModelFactory {

    private PlanModelFactory() {}

    public static Plan fromCRDSpec(PlanCRD planCRD, ApiCRDSpec api) {
        Plan plan = Plan.builder()
            .id(planCRD.getId())
            .name(planCRD.getName())
            .description(planCRD.getDescription())
            .characteristics(planCRD.getCharacteristics())
            .definitionVersion(DefinitionVersion.valueOf(api.getDefinitionVersion()))
            .crossId(planCRD.getCrossId())
            .excludedGroups(planCRD.getExcludedGroups())
            .generalConditions(planCRD.getGeneralConditions())
            .generalConditionsHrid(planCRD.getGeneralConditionsHrid())
            .order(planCRD.getOrder())
            .type(planCRD.getType())
            .validation(planCRD.getValidation())
            .apiType(ApiType.valueOf(api.getType()))
            .apiId(api.getId())
            .build();

        if (ApiType.NATIVE.getLabel().equalsIgnoreCase(api.getType())) {
            plan.setPlanDefinitionNativeV4(
                NativePlan.builder()
                    .security(planCRD.getSecurity())
                    .selectionRule(planCRD.getSelectionRule())
                    .status(planCRD.getStatus())
                    .tags(planCRD.getTags())
                    .mode(planCRD.getMode())
                    .name(planCRD.getName())
                    .build()
            );
        } else {
            plan.setPlanDefinitionHttpV4(
                io.gravitee.definition.model.v4.plan.Plan.builder()
                    .security(planCRD.getSecurity())
                    .selectionRule(planCRD.getSelectionRule())
                    .status(planCRD.getStatus())
                    .tags(planCRD.getTags())
                    .mode(planCRD.getMode())
                    .name(planCRD.getName())
                    .build()
            );
        }

        return plan;
    }

    public static Plan fromIntegration(IntegrationApi.Plan plan, Api federatedApi) {
        var id = generateFederatedPlanId(plan, federatedApi);
        var now = TimeProvider.now();
        return Plan.builder()
            .id(id)
            .name(plan.name())
            .description(plan.description())
            .apiId(federatedApi.getId())
            .federatedPlanDefinition(
                FederatedPlan.builder()
                    .id(id)
                    .providerId(plan.id())
                    .security(PlanSecurity.builder().type(PlanSecurityType.valueOf(plan.type().name()).getLabel()).build())
                    .status(PlanStatus.PUBLISHED)
                    .build()
            )
            .characteristics(plan.characteristics())
            .createdAt(now)
            .updatedAt(now)
            .validation(Plan.PlanValidationType.MANUAL)
            .build();
    }

    private static String generateFederatedPlanId(IntegrationApi.Plan plan, Api api) {
        return UuidString.generateForEnvironment(api.getId(), plan.id());
    }
}
