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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.rest.api.service.common.SecurityContextHelper.authenticateAs;
import static io.gravitee.rest.api.service.common.SecurityContextHelper.authenticateAsSystem;
import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.PARAM_API_PRODUCT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.model.ApiProduct;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.notification.ApiProductHook;
import io.gravitee.rest.api.service.notification.ApiProductTemplateModel;
import io.gravitee.rest.api.service.notification.HookScope;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ApiProductNotificationServiceImplTest {

    private static final ExecutionContext CTX = new ExecutionContext("org-1", "env-1");

    @Mock
    private NotifierService notifierService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private UserService userService;

    private ApiProductNotificationServiceImpl service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void buildService() {
        service = new ApiProductNotificationServiceImpl(notifierService, membershipService, userService);
    }

    @Test
    void should_expose_api_product_hook_metadata() {
        assertThat(ApiProductHook.API_PRODUCT_UPDATED.getScope()).isEqualTo(HookScope.API_PRODUCT);
        assertThat(ApiProductHook.API_PRODUCT_UPDATED.getLabel()).isEqualTo("API Product Updated");
        assertThat(ApiProductHook.API_PRODUCT_UPDATED.getCategory()).isEqualTo("LIFECYCLE");
        assertThat(ApiProductHook.API_PRODUCT_DEPLOYED.getScope()).isEqualTo(HookScope.API_PRODUCT);
        assertThat(ApiProductHook.API_PRODUCT_DEPLOYED.getLabel()).isEqualTo("API Product Deployed");
        assertThat(ApiProductHook.API_PRODUCT_DEPLOYED.getCategory()).isEqualTo("LIFECYCLE");
    }

    @Test
    void should_not_trigger_when_user_not_authenticated() {
        buildService();
        SecurityContextHolder.clearContext();

        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId("ap-1");
        apiProduct.setName("P1");
        apiProduct.setVersion("1.0");

        service.triggerUpdateNotification(CTX, apiProduct);

        verifyNoInteractions(notifierService);
        verifyNoInteractions(userService);
        verifyNoInteractions(membershipService);
    }

    @Test
    void should_not_trigger_when_authenticated_as_system() {
        buildService();
        authenticateAsSystem("system-user", Collections.emptySet());

        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId("ap-1");
        apiProduct.setName("P1");
        apiProduct.setVersion("1.0");

        service.triggerUpdateNotification(CTX, apiProduct);

        verifyNoInteractions(notifierService);
        verifyNoInteractions(userService);
        verifyNoInteractions(membershipService);
    }

    @Test
    void should_trigger_with_api_product_primary_owner_and_acting_user_when_authenticated() {
        buildService();

        UserEntity actor = new UserEntity();
        actor.setId("actor-1");
        actor.setEmail("actor@example.com");
        authenticateAs(actor);

        UserEntity loadedActor = new UserEntity();
        loadedActor.setId("actor-1");
        loadedActor.setEmail("actor@example.com");
        when(userService.findById(CTX, "actor-1")).thenReturn(loadedActor);

        when(membershipService.getPrimaryOwnerUserId("org-1", MembershipReferenceType.API_PRODUCT, "ap-1")).thenReturn("po-1");
        UserEntity poUser = new UserEntity();
        poUser.setId("po-1");
        poUser.setEmail("po@example.com");
        poUser.setFirstname("Pat");
        poUser.setLastname("Owner");
        when(userService.findById(CTX, "po-1")).thenReturn(poUser);

        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId("ap-1");
        apiProduct.setName("My Product");
        apiProduct.setVersion("2.0");

        service.triggerUpdateNotification(CTX, apiProduct);

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notifierService).trigger(eq(CTX), eq(ApiProductHook.API_PRODUCT_UPDATED), eq("ap-1"), paramsCaptor.capture());

        ApiProductTemplateModel model = (ApiProductTemplateModel) paramsCaptor.getValue().get(PARAM_API_PRODUCT);
        assertThat(model.getId()).isEqualTo("ap-1");
        assertThat(model.getName()).isEqualTo("My Product");
        assertThat(model.getVersion()).isEqualTo("2.0");
        assertThat(model.getPrimaryOwner()).isNotNull();
        assertThat(model.getPrimaryOwner().getId()).isEqualTo("po-1");

        assertThat(paramsCaptor.getValue().get(NotificationParamsBuilder.PARAM_USER)).isSameAs(loadedActor);
    }

    @Test
    void should_leave_primary_owner_null_when_not_resolvable() {
        buildService();

        UserEntity actor = new UserEntity();
        actor.setId("actor-1");
        authenticateAs(actor);
        when(userService.findById(CTX, "actor-1")).thenReturn(actor);

        when(membershipService.getPrimaryOwnerUserId("org-1", MembershipReferenceType.API_PRODUCT, "ap-x")).thenReturn("missing-po");
        when(userService.findById(CTX, "missing-po")).thenThrow(new NoSuchElementException());

        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId("ap-x");
        apiProduct.setName("Px");
        apiProduct.setVersion("1");

        service.triggerUpdateNotification(CTX, apiProduct);

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notifierService).trigger(eq(CTX), eq(ApiProductHook.API_PRODUCT_UPDATED), eq("ap-x"), paramsCaptor.capture());
        assertThat(((ApiProductTemplateModel) paramsCaptor.getValue().get(PARAM_API_PRODUCT)).getPrimaryOwner()).isNull();
    }

    @Test
    void should_trigger_deploy_notification_when_authenticated() {
        buildService();

        UserEntity actor = new UserEntity();
        actor.setId("actor-1");
        actor.setEmail("actor@example.com");
        authenticateAs(actor);
        when(userService.findById(CTX, "actor-1")).thenReturn(actor);
        when(membershipService.getPrimaryOwnerUserId("org-1", MembershipReferenceType.API_PRODUCT, "ap-deploy")).thenReturn(null);

        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId("ap-deploy");
        apiProduct.setName("Deploy Product");
        apiProduct.setVersion("3.0");

        service.triggerDeployNotification(CTX, apiProduct);

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notifierService).trigger(eq(CTX), eq(ApiProductHook.API_PRODUCT_DEPLOYED), eq("ap-deploy"), paramsCaptor.capture());
        ApiProductTemplateModel model = (ApiProductTemplateModel) paramsCaptor.getValue().get(PARAM_API_PRODUCT);
        assertThat(model.getId()).isEqualTo("ap-deploy");
        assertThat(model.getName()).isEqualTo("Deploy Product");
        assertThat(model.getVersion()).isEqualTo("3.0");
        assertThat(model.getPrimaryOwner()).isNull();
        assertThat(paramsCaptor.getValue().get(NotificationParamsBuilder.PARAM_USER)).isSameAs(actor);
    }
}
