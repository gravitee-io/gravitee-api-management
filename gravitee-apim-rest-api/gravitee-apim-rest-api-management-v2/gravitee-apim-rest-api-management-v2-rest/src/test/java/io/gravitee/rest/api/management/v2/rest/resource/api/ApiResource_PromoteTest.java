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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.promotion.model.PromotionStatus;
import io.gravitee.apim.core.promotion.use_case.CreatePromotionUseCase;
import io.gravitee.rest.api.management.v2.rest.model.PromotionRequest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiResource_PromoteTest extends ApiResourceTest {

    private static final String PROMOTION_ID = "promotion-id";
    private static final String FAKE_API_DEFINITION = "an API V4 export as JSON";
    private static final String SOURCE_ENV_ID = "source-env-id";
    private static final String SOURCE_ENV_NAME = "source-env-name";
    private static final String TARGET_API_ID = "target-api-id";
    private static final String TARGET_COCKPIT_ENV_ID = "cockpit-env-id";
    private static final String TARGET_COCKPIT_ENV_NAME = "cockpit-env-name";

    @Autowired
    private CreatePromotionUseCase createPromotionUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_promote";
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DEFINITION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(false);

        var promotionRequest = new PromotionRequest();
        promotionRequest.setTargetEnvName(TARGET_COCKPIT_ENV_NAME);
        promotionRequest.setTargetEnvCockpitId(TARGET_COCKPIT_ENV_ID);

        final Response response = rootTarget().request().post(Entity.json(promotionRequest));

        assertThat(response)
            .hasStatus(FORBIDDEN_403)
            .asError()
            .hasHttpStatus(FORBIDDEN_403)
            .hasMessage("You do not have sufficient rights to access this resource");
    }

    @Test
    public void should_return_promotion() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DEFINITION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(true);

        var createdAt = Instant.now();
        var createdPromotion = fixtures.core.model.PromotionFixtures.aPromotion()
            .toBuilder()
            .id(PROMOTION_ID)
            .apiId(API)
            .apiDefinition(FAKE_API_DEFINITION)
            .status(PromotionStatus.TO_BE_VALIDATED)
            .sourceEnvCockpitId(SOURCE_ENV_ID)
            .sourceEnvName(SOURCE_ENV_NAME)
            .targetApiId(TARGET_API_ID)
            .targetEnvCockpitId(TARGET_COCKPIT_ENV_ID)
            .targetEnvName(TARGET_COCKPIT_ENV_NAME)
            .createdAt(createdAt)
            .build();
        when(createPromotionUseCase.execute(any())).thenReturn(new CreatePromotionUseCase.Output(createdPromotion));

        var promotionRequest = new PromotionRequest();
        promotionRequest.setTargetEnvName(TARGET_COCKPIT_ENV_NAME);
        promotionRequest.setTargetEnvCockpitId(TARGET_COCKPIT_ENV_ID);
        final Response response = rootTarget().request().post(Entity.json(promotionRequest));

        assertThat(response.getStatus()).isEqualTo(OK_200);

        var promotion = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Promotion.class);
        assertThat(promotion).satisfies(p -> {
            assertThat(p.getId()).isEqualTo(PROMOTION_ID);
            assertThat(p.getApiId()).isEqualTo(API);
            assertThat(p.getApiDefinition()).isEqualTo(FAKE_API_DEFINITION);
            assertThat(p.getSourceEnvCockpitId()).isEqualTo(SOURCE_ENV_ID);
            assertThat(p.getSourceEnvName()).isEqualTo(SOURCE_ENV_NAME);
            assertThat(p.getTargetApiId()).isEqualTo(TARGET_API_ID);
            assertThat(p.getTargetEnvCockpitId()).isEqualTo(TARGET_COCKPIT_ENV_ID);
            assertThat(p.getTargetEnvName()).isEqualTo(TARGET_COCKPIT_ENV_NAME);
            assertThat(p.getCreatedAt()).isNotNull();
            assertThat(p.getCreatedAt().toInstant()).isEqualTo(createdAt);
        });
    }
}
