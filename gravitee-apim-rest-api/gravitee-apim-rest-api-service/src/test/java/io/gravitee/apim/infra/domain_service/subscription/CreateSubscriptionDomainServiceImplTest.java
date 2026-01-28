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
package io.gravitee.apim.infra.domain_service.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.model.SubscriptionConfiguration;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateSubscriptionDomainServiceImplTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";
    private static final String PLAN_ID = "plan-id";
    private static final String APP_ID = "app-id";

    @Mock
    private SubscriptionService subscriptionService;

    private CreateSubscriptionDomainServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new CreateSubscriptionDomainServiceImpl(subscriptionService);
    }

    @Test
    void should_delegate_to_subscription_service() {
        AuditInfo auditInfo = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();
        SubscriptionEntity created = new SubscriptionEntity();
        created.setId("sub-1");
        when(subscriptionService.create(any(ExecutionContext.class), any(NewSubscriptionEntity.class), eq(null))).thenReturn(created);

        io.gravitee.rest.api.model.SubscriptionEntity result = cut.create(
            auditInfo,
            PLAN_ID,
            APP_ID,
            "request-msg",
            null,
            null,
            Map.of("k", "v"),
            null,
            null
        );

        assertThat(result).isSameAs(created);
        ArgumentCaptor<ExecutionContext> ctxCaptor = ArgumentCaptor.forClass(ExecutionContext.class);
        ArgumentCaptor<NewSubscriptionEntity> entityCaptor = ArgumentCaptor.forClass(NewSubscriptionEntity.class);
        verify(subscriptionService).create(ctxCaptor.capture(), entityCaptor.capture(), eq(null));
        assertThat(ctxCaptor.getValue().getOrganizationId()).isEqualTo(ORG_ID);
        assertThat(ctxCaptor.getValue().getEnvironmentId()).isEqualTo(ENV_ID);
        assertThat(entityCaptor.getValue().getPlan()).isEqualTo(PLAN_ID);
        assertThat(entityCaptor.getValue().getApplication()).isEqualTo(APP_ID);
        assertThat(entityCaptor.getValue().getRequest()).isEqualTo("request-msg");
        assertThat(entityCaptor.getValue().getMetadata()).isEqualTo(Map.of("k", "v"));
    }

    @Test
    void should_map_configuration_to_entity() {
        AuditInfo auditInfo = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();
        SubscriptionConfiguration config = SubscriptionConfiguration.builder()
            .entrypointId("ep-id")
            .channel("channel")
            .entrypointConfiguration("{\"key\":\"value\"}")
            .build();
        when(subscriptionService.create(any(), any(), any())).thenReturn(new SubscriptionEntity());

        cut.create(auditInfo, PLAN_ID, APP_ID, null, null, config, null, null, null);

        ArgumentCaptor<NewSubscriptionEntity> entityCaptor = ArgumentCaptor.forClass(NewSubscriptionEntity.class);
        verify(subscriptionService).create(any(), entityCaptor.capture(), any());
        assertThat(entityCaptor.getValue().getConfiguration()).isNotNull();
        assertThat(entityCaptor.getValue().getConfiguration().getEntrypointId()).isEqualTo("ep-id");
        assertThat(entityCaptor.getValue().getConfiguration().getChannel()).isEqualTo("channel");
        assertThat(entityCaptor.getValue().getConfiguration().getEntrypointConfiguration()).isEqualTo("{\"key\":\"value\"}");
    }
}
