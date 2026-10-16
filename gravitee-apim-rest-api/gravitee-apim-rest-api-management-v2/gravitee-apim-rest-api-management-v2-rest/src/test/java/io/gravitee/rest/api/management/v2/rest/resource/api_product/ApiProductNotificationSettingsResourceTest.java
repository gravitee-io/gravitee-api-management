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
package io.gravitee.rest.api.management.v2.rest.resource.api_product;

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.use_case.GetApiProductsUseCase;
import io.gravitee.apim.core.api_product.use_case.VerifyApiProductExistsUseCase;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ApiProductNotificationSettingsResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";

    @Inject
    private VerifyApiProductExistsUseCase verifyApiProductExistsUseCase;

    @Inject
    private PortalNotificationConfigService portalNotificationConfigService;

    @Inject
    private GenericNotificationConfigService genericNotificationConfigService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/api-products/" + API_PRODUCT_ID + "/notificationSettings";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENV_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(verifyApiProductExistsUseCase, portalNotificationConfigService, genericNotificationConfigService);
    }

    private void givenApiProductMissing() {
        doThrow(new ApiProductNotFoundException(API_PRODUCT_ID)).when(verifyApiProductExistsUseCase).execute(any());
    }

    @Nested
    class GetApiProductNotificationSettingsTest {

        @Test
        void should_return_notification_settings() {
            var portalConfig = new PortalNotificationConfigEntity();
            portalConfig.setReferenceId(API_PRODUCT_ID);
            when(portalNotificationConfigService.findById(any(), any(), any())).thenReturn(portalConfig);
            when(genericNotificationConfigService.findByReference(any(), any())).thenReturn(List.of(new GenericNotificationConfigEntity()));

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
        }

        @Test
        void should_return_404_when_api_product_not_found() {
            givenApiProductMissing();

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }
    }

    @Nested
    class CreateApiProductNotificationSettingsTest {

        @Test
        void should_take_organization_from_context_ignoring_body() {
            when(genericNotificationConfigService.create(any())).thenAnswer(i -> i.getArguments()[0]);
            var config = GenericNotificationConfigEntity.builder()
                .configType(NotificationConfigType.GENERIC)
                .name("my-notifier")
                .notifier("webhook")
                .referenceType(NotificationReferenceType.API_PRODUCT.name())
                .referenceId(API_PRODUCT_ID)
                .organizationId("other-org")
                .build();

            Response response = rootTarget().request().post(Entity.json(config));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var captor = ArgumentCaptor.forClass(GenericNotificationConfigEntity.class);
            verify(genericNotificationConfigService).create(captor.capture());
            assertThat(captor.getValue().getOrganizationId()).isEqualTo(ORGANIZATION);
        }
    }
}
