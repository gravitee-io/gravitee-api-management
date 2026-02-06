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

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.management.v2.rest.model.CreatePlanV2;
import io.gravitee.rest.api.management.v2.rest.model.CreatePlanV4;
import io.gravitee.rest.api.management.v2.rest.model.PlanValidation;
import io.gravitee.rest.api.management.v2.rest.model.UpdateGenericPlanSecurity;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePlanFederated;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePlanV2;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePlanV4;
import io.gravitee.rest.api.model.v4.nativeapi.NativePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanFixtures {

    private PlanFixtures() {}

    private static final Supplier<UpdateGenericPlanSecurity> BASE_UPDATE_PLAN_SECURITY = () ->
        new UpdateGenericPlanSecurity().configuration("{\"nice\": \"config\"}");

    private static final Supplier<io.gravitee.rest.api.management.v2.rest.model.PlanSecurity> BASE_PLAN_SECURITY = () ->
        new io.gravitee.rest.api.management.v2.rest.model.PlanSecurity()
            .type(io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType.API_KEY)
            .configuration("{\"nice\": \"config\"}");

    private static final Supplier<CreatePlanV4> BASE_CREATE_PLAN_HTTP_V4 = () ->
        (CreatePlanV4) new CreatePlanV4()
            .flows(List.of(FlowFixtures.aFlowHttpV4()))
            .name("My plan")
            .description("Description")
            .order(1)
            .characteristics(List.of("characteristic1", "characteristic2"))
            .commentRequired(true)
            .commentMessage("Comment message")
            .crossId("my-plan-crossId")
            .generalConditions("General conditions")
            .tags(List.of("tag1", "tag2"))
            .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
            .validation(PlanValidation.AUTO)
            .selectionRule("{#request.attribute['selectionRule'] != null}")
            .security(BASE_PLAN_SECURITY.get());

    private static final Supplier<CreatePlanV4> BASE_CREATE_PLAN_NATIVE_V4 = () ->
        (CreatePlanV4) new CreatePlanV4()
            .flows(List.of(FlowFixtures.aFlowNativeV4()))
            .name("My plan")
            .description("Description")
            .order(1)
            .characteristics(List.of("characteristic1", "characteristic2"))
            .commentRequired(true)
            .commentMessage("Comment message")
            .crossId("my-plan-crossId")
            .generalConditions("General conditions")
            .tags(List.of("tag1", "tag2"))
            .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
            .validation(PlanValidation.AUTO)
            .selectionRule("{#request.attribute['selectionRule'] != null}")
            .security(BASE_PLAN_SECURITY.get());

    private static final Supplier<CreatePlanV2> BASE_CREATE_PLAN_V2 = () ->
        (CreatePlanV2) new CreatePlanV2()
            .flows(List.of(FlowFixtures.aFlowV2()))
            .name("My plan")
            .description("Description")
            .order(1)
            .characteristics(List.of("characteristic1", "characteristic2"))
            .commentMessage("Comment message")
            .crossId("my-plan-crossId")
            .generalConditions("General conditions")
            .tags(List.of("tag1", "tag2"))
            .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
            .validation(PlanValidation.AUTO)
            .selectionRule("{#request.attribute['selectionRule'] != null}")
            .security(BASE_PLAN_SECURITY.get());

    private static final Supplier<UpdatePlanV4> BASE_UPDATE_PLAN_V4 = () ->
        (UpdatePlanV4) new UpdatePlanV4()
            .flows(List.of(FlowFixtures.aFlowHttpV4()))
            .tags(List.of("tag1", "tag2"))
            .selectionRule("{#request.attribute['selectionRule'] != null}")
            .name("My plan")
            .description("Description")
            .order(1)
            .characteristics(List.of("characteristic1", "characteristic2"))
            .commentMessage("Comment message")
            .crossId("my-plan-crossId")
            .generalConditions("General conditions")
            .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
            .validation(PlanValidation.AUTO)
            .security(BASE_UPDATE_PLAN_SECURITY.get());

    private static final Supplier<UpdatePlanV4> BASE_UPDATE_NATIVE_PLAN_V4 = () ->
        (UpdatePlanV4) new UpdatePlanV4()
            .flows(List.of(FlowFixtures.aFlowNativeV4()))
            .tags(List.of("tag1", "tag2"))
            .selectionRule("{#request.attribute['selectionRule'] != null}")
            .name("My plan")
            .description("Description")
            .order(1)
            .characteristics(List.of("characteristic1", "characteristic2"))
            .commentMessage("Comment message")
            .crossId("my-plan-crossId")
            .generalConditions("General conditions")
            .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
            .validation(PlanValidation.AUTO)
            .security(BASE_UPDATE_PLAN_SECURITY.get());

    private static final Supplier<UpdatePlanFederated> BASE_UPDATE_PLAN_FEDERATED = () ->
        (UpdatePlanFederated) new UpdatePlanFederated()
            .name("My plan")
            .description("Description")
            .order(1)
            .characteristics(List.of("characteristic1", "characteristic2"))
            .commentMessage("Comment message")
            .crossId("my-plan-crossId")
            .generalConditions("General conditions")
            .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
            .validation(PlanValidation.AUTO)
            .commentRequired(false)
            .security(BASE_UPDATE_PLAN_SECURITY.get());

    private static final Supplier<UpdatePlanV2> BASE_UPDATE_PLAN_V2 = () ->
        (UpdatePlanV2) new UpdatePlanV2()
            .flows(List.of(FlowFixtures.aFlowV2()))
            .tags(List.of("tag1", "tag2"))
            .selectionRule("{#request.attribute['selectionRule'] != null}")
            .name("My plan")
            .description("Description")
            .order(1)
            .characteristics(List.of("characteristic1", "characteristic2"))
            .commentMessage("Comment message")
            .crossId("my-plan-crossId")
            .generalConditions("General conditions")
            .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
            .validation(PlanValidation.AUTO)
            .security(BASE_UPDATE_PLAN_SECURITY.get());

    private static final io.gravitee.rest.api.model.PlanEntity.PlanEntityBuilder BASE_PLAN_ENTITY_V2 =
        io.gravitee.rest.api.model.PlanEntity.builder()
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
            .flows(List.of(FlowFixtures.aModelFlowV2()));

    public static CreatePlanV4 aCreatePlanHttpV4() {
        return BASE_CREATE_PLAN_HTTP_V4.get();
    }

    public static CreatePlanV4 aCreatePlanNativeV4() {
        return BASE_CREATE_PLAN_NATIVE_V4.get();
    }

    public static CreatePlanV2 aCreatePlanV2() {
        return BASE_CREATE_PLAN_V2.get();
    }

    public static UpdatePlanV4 anUpdatePlanV4() {
        return BASE_UPDATE_PLAN_V4.get();
    }

    public static UpdatePlanV4 anUpdatePlanNativeV4() {
        return BASE_UPDATE_NATIVE_PLAN_V4.get();
    }

    public static UpdatePlanV2 anUpdatePlanV2() {
        return BASE_UPDATE_PLAN_V2.get();
    }

    public static UpdatePlanFederated anUpdatePlanFederated() {
        return BASE_UPDATE_PLAN_FEDERATED.get();
    }

    public static PlanEntity aPlanEntityV4() {
        return PlanModelFixtures.aPlanEntityV4();
    }

    public static NativePlanEntity aNativePlanEntityV4() {
        return PlanModelFixtures.aNativePlanEntityV4();
    }

    public static io.gravitee.rest.api.model.PlanEntity aPlanEntityV2() {
        return PlanModelFixtures.aPlanEntityV2();
    }

    public static PlanWithFlows aPlanWithHttpFlows() {
        return PlanWithFlows.builder()
            .id("id")
            .crossId("my-plan-crossId")
            .name("My plan")
            .description("Description")
            .validation(Plan.PlanValidationType.AUTO)
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .apiType(ApiType.MESSAGE)
            .definitionVersion(DefinitionVersion.V4)
            .planDefinitionHttpV4(
                fixtures.definition.PlanFixtures.HttpV4Definition.anApiKeyV4()
                    .toBuilder()
                    .security(PlanSecurity.builder().type("API_KEY").configuration("{\"nice\": \"config\"}").build())
                    .selectionRule("{#request.attribute['selectionRule'] != null}")
                    .tags(Set.of("tag1", "tag2"))
                    .status(PlanStatus.CLOSED)
                    .flows(List.of(FlowFixtures.aModelFlowHttpV4()))
                    .build()
            )
            .referenceId("api-id")
            .order(1)
            .characteristics(List.of("characteristic1", "characteristic2"))
            .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
            .commentMessage("Comment message")
            .commentRequired(true)
            .generalConditions("General conditions")
            .flows(List.of(FlowFixtures.aModelFlowHttpV4()))
            .build();
    }

    public static PlanWithFlows aPlanWithNativeFlows() {
        return PlanWithFlows.builder()
            .id("id")
            .crossId("my-plan-crossId")
            .name("My plan")
            .description("Description")
            .validation(Plan.PlanValidationType.AUTO)
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .apiType(ApiType.NATIVE)
            .definitionVersion(DefinitionVersion.V4)
            .planDefinitionNativeV4(
                fixtures.definition.PlanFixtures.NativeV4Definition.anApiKeyV4()
                    .toBuilder()
                    .security(PlanSecurity.builder().type("API_KEY").configuration("{\"nice\": \"config\"}").build())
                    .selectionRule("{#request.attribute['selectionRule'] != null}")
                    .tags(Set.of("tag1", "tag2"))
                    .status(PlanStatus.CLOSED)
                    .flows(List.of(FlowFixtures.aModelFlowNativeV4()))
                    .build()
            )
            .referenceId("api-id")
            .order(1)
            .characteristics(List.of("characteristic1", "characteristic2"))
            .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
            .commentMessage("Comment message")
            .commentRequired(true)
            .generalConditions("General conditions")
            .flows(List.of(FlowFixtures.aModelFlowNativeV4()))
            .build();
    }
}
