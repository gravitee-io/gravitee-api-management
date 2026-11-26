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
package io.gravitee.apim.infra.domain_service.alert;

import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiDefaultAlertsDomainServiceLegacyWrapperTest {

    @Mock
    AlertService alertService;

    @InjectMocks
    ApiDefaultAlertsDomainServiceLegacyWrapper service;

    @Test
    void should_create_default_alerts_for_the_api_in_the_audit_context() {
        var auditInfo = AuditInfo.builder()
            .organizationId("org-id")
            .environmentId("env-id")
            .actor(AuditActor.builder().userId("user-id").build())
            .build();

        service.createDefaultAlerts("api-id", auditInfo);

        verify(alertService).createDefaults(new ExecutionContext("org-id", "env-id"), AlertReferenceType.API, "api-id");
    }
}
