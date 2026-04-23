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
package io.gravitee.apim.core.subscription.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec;
import io.gravitee.definition.model.DefinitionVersion;
import org.junit.jupiter.api.Test;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
class ValidateSubscriptionCRDDomainServiceTest {

    private static final String ORGANIZATION_ID = "org-id";
    private static final String ENVIRONMENT_ID = "env-id";
    private static final String PLAN_ID = "plan-id";
    private final PlanCrudService planCrudService = mock(PlanCrudService.class);
    private final ValidateSubscriptionCRDDomainService cut = new ValidateSubscriptionCRDDomainService(planCrudService);

    @Test
    void should_return_severe_error_when_custom_api_key_is_set_on_non_api_key_plan() {
        var spec = aSpec().customApiKey("my-custom-key").build();
        when(planCrudService.getById(PLAN_ID)).thenReturn(v2PlanWith("JWT"));

        var result = cut.validateAndSanitize(
            new ValidateSubscriptionCRDDomainService.Input(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, "user-id"), spec)
        );

        assertThat(result.severe()).isPresent();
        assertThat(result.severe().orElseThrow().getFirst().getMessage()).contains("customApiKey is only allowed for API_KEY plans");
    }

    @Test
    void should_accept_custom_api_key_on_api_key_plan() {
        var spec = aSpec().customApiKey("my-custom-key").build();
        when(planCrudService.getById(PLAN_ID)).thenReturn(v2PlanWith("API_KEY"));

        var result = cut.validateAndSanitize(
            new ValidateSubscriptionCRDDomainService.Input(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, "user-id"), spec)
        );

        assertThat(result.severe()).isEmpty();
        assertThat(result.value()).isPresent();
        assertThat(result.value().orElseThrow().spec()).isEqualTo(spec);
    }

    private static SubscriptionCRDSpec.SubscriptionCRDSpecBuilder aSpec() {
        return SubscriptionCRDSpec.builder()
            .id("subscription-id")
            .referenceId("api-id")
            .referenceType(SubscriptionReferenceType.API)
            .applicationId("application-id")
            .planId(PLAN_ID);
    }

    private static Plan v2PlanWith(String security) {
        return Plan.builder()
            .id(PLAN_ID)
            .apiId("api-id")
            .definitionVersion(DefinitionVersion.V2)
            .planDefinitionV2(io.gravitee.definition.model.Plan.builder().security(security).build())
            .build();
    }
}
