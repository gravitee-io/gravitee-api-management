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
package io.gravitee.apim.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application.domain_service.ImportApplicationCRDDomainService;
import io.gravitee.apim.core.application.use_case.UpdateApplicationUseCase;
import io.gravitee.apim.core.application_certificate.domain_service.MtlsSubscriptionSyncDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateApplicationUseCaseTest {

    private static final String APPLICATION_ID = "app-id";

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("org-id")
        .environmentId("env-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    @Mock
    private ImportApplicationCRDDomainService importApplicationCRDDomainService;

    @Mock
    private MtlsSubscriptionSyncDomainService mtlsSubscriptionSyncDomainService;

    private UpdateApplicationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateApplicationUseCase(importApplicationCRDDomainService, mtlsSubscriptionSyncDomainService);
    }

    @Test
    void should_update_application() {
        var updateEntity = new UpdateApplicationEntity();
        updateEntity.setName("Updated App");

        var expectedApp = new ApplicationEntity();
        expectedApp.setId(APPLICATION_ID);
        expectedApp.setName("Updated App");

        when(importApplicationCRDDomainService.update(eq(APPLICATION_ID), eq(updateEntity), eq(AUDIT_INFO))).thenReturn(expectedApp);

        var output = useCase.execute(new UpdateApplicationUseCase.Input(AUDIT_INFO, APPLICATION_ID, updateEntity));

        assertThat(output.application().getId()).isEqualTo(APPLICATION_ID);
        assertThat(output.application().getName()).isEqualTo("Updated App");
    }

    @Test
    void should_sync_mtls_subscriptions_after_update() {
        var updateEntity = new UpdateApplicationEntity();
        var expectedApp = new ApplicationEntity();
        expectedApp.setId(APPLICATION_ID);

        when(importApplicationCRDDomainService.update(eq(APPLICATION_ID), eq(updateEntity), eq(AUDIT_INFO))).thenReturn(expectedApp);

        useCase.execute(new UpdateApplicationUseCase.Input(AUDIT_INFO, APPLICATION_ID, updateEntity));

        verify(mtlsSubscriptionSyncDomainService).updateActiveMTLSSubscriptions(APPLICATION_ID);
    }

    @Test
    void should_propagate_exception_when_application_not_found() {
        var updateEntity = new UpdateApplicationEntity();

        when(importApplicationCRDDomainService.update(eq(APPLICATION_ID), eq(updateEntity), eq(AUDIT_INFO))).thenThrow(
            new ApplicationNotFoundException(APPLICATION_ID)
        );

        assertThatThrownBy(() ->
            useCase.execute(new UpdateApplicationUseCase.Input(AUDIT_INFO, APPLICATION_ID, updateEntity))
        ).isInstanceOf(ApplicationNotFoundException.class);
    }
}
