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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloseSubscriptionDomainServiceTest {

    private static final String SUB_ID = "sub-id";
    private static final String API_ID = "api-id";
    private static final String API_PRODUCT_ID = "api-product-id";
    private static final String APP_ID = "app-id";
    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();

    @Mock
    private SubscriptionCrudService subscriptionCrudService;

    @Mock
    private RejectSubscriptionDomainService rejectSubscriptionDomainService;

    @Mock
    private io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService triggerNotificationDomainService;

    @Mock
    private io.gravitee.apim.core.audit.domain_service.AuditDomainService auditDomainService;

    @Mock
    private io.gravitee.apim.core.application.crud_service.ApplicationCrudService applicationCrudService;

    @Mock
    private io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService revokeApiKeyDomainService;

    @Mock
    private io.gravitee.apim.core.api.crud_service.ApiCrudService apiCrudService;

    @Mock
    private io.gravitee.apim.core.integration.service_provider.IntegrationAgent integrationAgent;

    private CloseSubscriptionDomainService cut;

    @BeforeEach
    void setUp() {
        cut = new CloseSubscriptionDomainService(
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

    private static SubscriptionEntity subscription(String id, SubscriptionEntity.Status status) {
        return SubscriptionEntity.builder()
            .id(id)
            .apiId(API_ID)
            .applicationId(APP_ID)
            .planId("plan-1")
            .status(status)
            .referenceId(API_PRODUCT_ID)
            .createdAt(ZonedDateTime.now())
            .updatedAt(ZonedDateTime.now())
            .build();
    }

    @Nested
    class CloseSubscriptionWithApi {

        @Test
        void should_return_subscription_as_is_when_already_closed() {
            SubscriptionEntity sub = subscription(SUB_ID, SubscriptionEntity.Status.CLOSED);
            Api api = Api.builder().id(API_ID).build();

            SubscriptionEntity result = cut.closeSubscription(sub, api, AUDIT_INFO);

            assertThat(result).isSameAs(sub);
        }

        @Test
        void should_return_subscription_as_is_when_rejected() {
            SubscriptionEntity sub = subscription(SUB_ID, SubscriptionEntity.Status.REJECTED);
            Api api = Api.builder().id(API_ID).build();

            SubscriptionEntity result = cut.closeSubscription(sub, api, AUDIT_INFO);

            assertThat(result).isSameAs(sub);
        }

        @Test
        void should_reject_when_pending() {
            SubscriptionEntity sub = subscription(SUB_ID, SubscriptionEntity.Status.PENDING);
            SubscriptionEntity rejected = sub.toBuilder().status(SubscriptionEntity.Status.REJECTED).build();
            Api api = Api.builder().id(API_ID).build();
            when(rejectSubscriptionDomainService.reject(eq(sub), eq("Subscription has been closed."), eq(AUDIT_INFO))).thenReturn(rejected);

            SubscriptionEntity result = cut.closeSubscription(sub, api, AUDIT_INFO);

            assertThat(result).isSameAs(rejected);
            verify(rejectSubscriptionDomainService).reject(eq(sub), eq("Subscription has been closed."), eq(AUDIT_INFO));
        }
    }

    @Nested
    class CloseSubscriptionForApiProduct {

        @Test
        void should_return_subscription_as_is_when_already_closed() {
            SubscriptionEntity sub = subscription(SUB_ID, SubscriptionEntity.Status.CLOSED);

            SubscriptionEntity result = cut.closeSubscriptionForApiProduct(sub, AUDIT_INFO);

            assertThat(result).isSameAs(sub);
        }

        @Test
        void should_return_subscription_as_is_when_rejected() {
            SubscriptionEntity sub = subscription(SUB_ID, SubscriptionEntity.Status.REJECTED);

            SubscriptionEntity result = cut.closeSubscriptionForApiProduct(sub, AUDIT_INFO);

            assertThat(result).isSameAs(sub);
        }

        @Test
        void should_reject_when_pending() {
            SubscriptionEntity sub = subscription(SUB_ID, SubscriptionEntity.Status.PENDING);
            SubscriptionEntity rejected = sub.toBuilder().status(SubscriptionEntity.Status.REJECTED).build();
            when(rejectSubscriptionDomainService.reject(eq(sub), eq("Subscription has been closed."), eq(AUDIT_INFO))).thenReturn(rejected);

            SubscriptionEntity result = cut.closeSubscriptionForApiProduct(sub, AUDIT_INFO);

            assertThat(result).isSameAs(rejected);
            verify(rejectSubscriptionDomainService).reject(eq(sub), eq("Subscription has been closed."), eq(AUDIT_INFO));
        }

        @Test
        void should_close_and_create_audit_when_accepted() {
            SubscriptionEntity sub = subscription(SUB_ID, SubscriptionEntity.Status.ACCEPTED);
            SubscriptionEntity closed = sub.toBuilder().status(SubscriptionEntity.Status.CLOSED).updatedAt(ZonedDateTime.now()).build();
            when(subscriptionCrudService.update(any())).thenReturn(closed);
            BaseApplicationEntity application = mock(BaseApplicationEntity.class);
            when(application.hasApiKeySharedMode()).thenReturn(true);
            when(applicationCrudService.findById(any(ExecutionContext.class), eq(APP_ID))).thenReturn(application);

            SubscriptionEntity result = cut.closeSubscriptionForApiProduct(sub, AUDIT_INFO);

            assertThat(result).isSameAs(closed);
            ArgumentCaptor<SubscriptionEntity> updateCaptor = ArgumentCaptor.forClass(SubscriptionEntity.class);
            verify(subscriptionCrudService).update(updateCaptor.capture());
            assertThat(updateCaptor.getValue().getStatus()).isEqualTo(SubscriptionEntity.Status.CLOSED);
            verify(auditDomainService).createApiProductAuditLog(any());
            verify(auditDomainService).createApplicationAuditLog(any());
        }
    }
}
