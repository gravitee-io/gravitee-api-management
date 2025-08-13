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
package io.gravitee.apim.infra.domain_service.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ImportApplicationCRDDomainServiceLegacyWrapperTest {

    @Mock
    ApplicationService applicationService;

    @InjectMocks
    ImportApplicationCRDDomainServiceLegacyWrapper service;

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String APP_ID = UuidString.generateRandom();
    private static final String APP_NAME = "test_app";
    private static final String APP_DESCRIPTION = "test_app_description";

    @Test
    void should_call_legacy_service_for_creation() {
        NewApplicationEntity newApplicationEntity = new NewApplicationEntity();
        newApplicationEntity.setName(APP_NAME);
        newApplicationEntity.setDescription(APP_DESCRIPTION);

        service.create(newApplicationEntity, new AuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, new AuditActor("test_user", "test", "test")));

        verify(applicationService).create(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID), newApplicationEntity, "test_user");
    }

    @Test
    void should_call_legacy_service_for_update() {
        UpdateApplicationEntity updateApplicationEntity = new UpdateApplicationEntity();
        updateApplicationEntity.setName(APP_NAME);
        updateApplicationEntity.setDescription(APP_DESCRIPTION);

        when(applicationService.findById(any(), eq(APP_ID))).thenReturn(new io.gravitee.rest.api.model.ApplicationEntity());

        service.update(
            APP_ID,
            updateApplicationEntity,
            new AuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, new AuditActor("test_user", "test", "test"))
        );

        verify(applicationService).update(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID), APP_ID, updateApplicationEntity);
    }
}
