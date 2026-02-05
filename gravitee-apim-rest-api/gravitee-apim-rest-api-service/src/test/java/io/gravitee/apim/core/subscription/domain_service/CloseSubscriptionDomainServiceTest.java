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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity.Status;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CloseSubscriptionDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId(USER_ID).build())
        .build();

    private static final String SUBSCRIPTION_ID = "subscription-id";
    private static final String APPLICATION_ID = "application-id";
    private static final String PLAN_ID = "plan-id";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";

    @Mock
    SubscriptionCrudService subscriptionCrudService;

    @Mock
    RejectSubscriptionDomainService rejectSubscriptionDomainService;

    @Mock
    TriggerNotificationDomainService triggerNotificationDomainService;

    @Mock
    AuditDomainService auditDomainService;

    @Mock
    ApplicationCrudService applicationCrudService;

    @Mock
    RevokeApiKeyDomainService revokeApiKeyDomainService;

    @Mock
    ApiCrudService apiCrudService;

    @Mock
    IntegrationAgent integrationAgent;

    CloseSubscriptionDomainService service;

    @BeforeEach
    void setUp() {
        service = new CloseSubscriptionDomainService(
            subscriptionCrudService,
            applicationCrudService,
            auditDomainService,
            triggerNotificationDomainService,
            rejectSubscriptionDomainService,
            revokeApiKeyDomainService,
            apiCrudService,
            integrationAgent
        );
    }

    @Test
    void closeSubscriptionForApiProduct_should_close_accepted_subscription_and_create_audit_and_revoke_keys() {
        var subscription = SubscriptionEntity.builder()
            .id(SUBSCRIPTION_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .applicationId(APPLICATION_ID)
            .planId(PLAN_ID)
            .status(Status.ACCEPTED)
            .build();
        var closedSubscription = subscription.close();

        when(subscriptionCrudService.update(any(SubscriptionEntity.class))).thenReturn(closedSubscription);
        when(applicationCrudService.findById(any(ExecutionContext.class), eq(APPLICATION_ID))).thenReturn(
            BaseApplicationEntity.builder().id(APPLICATION_ID).apiKeyMode(ApiKeyMode.EXCLUSIVE).build()
        );
        when(revokeApiKeyDomainService.revokeAllSubscriptionsApiKeys(any(SubscriptionEntity.class), eq(AUDIT_INFO))).thenReturn(
            Collections.emptySet()
        );

        var result = service.closeSubscription(subscription, AUDIT_INFO);

        assertThat(result.getStatus()).isEqualTo(Status.CLOSED);
        verify(subscriptionCrudService).update(any(SubscriptionEntity.class));
        verify(auditDomainService).createApiProductAuditLog(any());
        verify(auditDomainService).createApplicationAuditLog(any());
        verify(triggerNotificationDomainService, never()).triggerApiNotification(anyString(), anyString(), any());
        verify(triggerNotificationDomainService, never()).triggerApplicationNotification(anyString(), anyString(), any());
    }

    @Test
    void closeSubscriptionForApiProduct_should_reject_pending_subscription() {
        var subscription = SubscriptionEntity.builder()
            .id(SUBSCRIPTION_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .applicationId(APPLICATION_ID)
            .planId(PLAN_ID)
            .status(Status.PENDING)
            .build();

        var rejectedSubscription = subscription.rejectBy(USER_ID, "Subscription has been closed.");
        when(subscriptionCrudService.update(any(SubscriptionEntity.class))).thenReturn(rejectedSubscription);

        var result = service.closeSubscription(subscription, AUDIT_INFO);

        assertThat(result.getStatus()).isEqualTo(Status.REJECTED);
        verify(subscriptionCrudService).update(any());
        verify(auditDomainService).createApiProductAuditLog(any());
        verify(auditDomainService).createApplicationAuditLog(any());
    }

    @Test
    void closeSubscriptionForApiProduct_by_id_should_load_subscription_then_close() {
        var subscription = SubscriptionEntity.builder()
            .id(SUBSCRIPTION_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .applicationId(APPLICATION_ID)
            .planId(PLAN_ID)
            .status(Status.ACCEPTED)
            .build();

        when(subscriptionCrudService.get(eq(SUBSCRIPTION_ID))).thenReturn(subscription);
        when(subscriptionCrudService.update(any(SubscriptionEntity.class))).thenReturn(subscription.close());
        when(applicationCrudService.findById(any(ExecutionContext.class), eq(APPLICATION_ID))).thenReturn(
            BaseApplicationEntity.builder().id(APPLICATION_ID).apiKeyMode(ApiKeyMode.EXCLUSIVE).build()
        );

        var result = service.closeSubscription(SUBSCRIPTION_ID, AUDIT_INFO);

        assertThat(result.getStatus()).isEqualTo(Status.CLOSED);
        verify(subscriptionCrudService).get(eq(SUBSCRIPTION_ID));
    }
}
