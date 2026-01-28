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
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.ZonedDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateSubscriptionDomainServiceImplTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";
    private static final String SUB_ID = "sub-id";

    @Mock
    private SubscriptionService subscriptionService;

    private UpdateSubscriptionDomainServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new UpdateSubscriptionDomainServiceImpl(subscriptionService);
    }

    @Test
    void should_delegate_to_subscription_service() {
        AuditInfo auditInfo = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();
        SubscriptionConfigurationEntity config = SubscriptionConfigurationEntity.builder().entrypointId("ep").build();
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = ZonedDateTime.now().plusDays(1);

        cut.update(auditInfo, SUB_ID, config, Map.of("k", "v"), start, end);

        ArgumentCaptor<ExecutionContext> ctxCaptor = ArgumentCaptor.forClass(ExecutionContext.class);
        ArgumentCaptor<UpdateSubscriptionEntity> entityCaptor = ArgumentCaptor.forClass(UpdateSubscriptionEntity.class);
        verify(subscriptionService).update(ctxCaptor.capture(), entityCaptor.capture());
        assertThat(ctxCaptor.getValue().getOrganizationId()).isEqualTo(ORG_ID);
        assertThat(ctxCaptor.getValue().getEnvironmentId()).isEqualTo(ENV_ID);
        assertThat(entityCaptor.getValue().getId()).isEqualTo(SUB_ID);
        assertThat(entityCaptor.getValue().getConfiguration()).isSameAs(config);
        assertThat(entityCaptor.getValue().getMetadata()).isEqualTo(Map.of("k", "v"));
        assertThat(entityCaptor.getValue().getStartingAt()).isNotNull();
        assertThat(entityCaptor.getValue().getEndingAt()).isNotNull();
    }

    @Test
    void should_handle_null_optional_fields() {
        AuditInfo auditInfo = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();

        cut.update(auditInfo, SUB_ID, null, null, null, null);

        ArgumentCaptor<UpdateSubscriptionEntity> entityCaptor = ArgumentCaptor.forClass(UpdateSubscriptionEntity.class);
        verify(subscriptionService).update(any(), entityCaptor.capture());
        assertThat(entityCaptor.getValue().getId()).isEqualTo(SUB_ID);
        assertThat(entityCaptor.getValue().getConfiguration()).isNull();
        assertThat(entityCaptor.getValue().getMetadata()).isNull();
        assertThat(entityCaptor.getValue().getStartingAt()).isNull();
        assertThat(entityCaptor.getValue().getEndingAt()).isNull();
    }
}
