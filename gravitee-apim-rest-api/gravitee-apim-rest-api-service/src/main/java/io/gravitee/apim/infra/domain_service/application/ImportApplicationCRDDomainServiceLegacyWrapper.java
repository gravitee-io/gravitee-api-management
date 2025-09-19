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

import io.gravitee.apim.core.application.domain_service.ImportApplicationCRDDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import org.springframework.stereotype.Service;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
public class ImportApplicationCRDDomainServiceLegacyWrapper implements ImportApplicationCRDDomainService {

    private final ApplicationService applicationService;

    public ImportApplicationCRDDomainServiceLegacyWrapper(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public BaseApplicationEntity create(NewApplicationEntity newApplicationEntity, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        return applicationService.create(executionContext, newApplicationEntity, auditInfo.actor().userId());
    }

    @Override
    public BaseApplicationEntity update(String applicationId, UpdateApplicationEntity updateApplicationEntity, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        ApplicationEntity existing = applicationService.findById(executionContext, applicationId);
        boolean restored = false;
        if (
            ApplicationStatus.ARCHIVED.name().equals(existing.getStatus()) &&
            ApplicationStatus.ACTIVE.name().equals(updateApplicationEntity.getStatus())
        ) {
            applicationService.restore(executionContext, applicationId);
            restored = true;
        }
        try {
            return applicationService.update(executionContext, applicationId, updateApplicationEntity);
        } catch (AbstractManagementException ame) {
            if (restored) {
                // manual rollback to avoid a commit on AbstractManagementException that can happened on a restored apps
                applicationService.archive(executionContext, applicationId);
            }
            throw ame;
        }
    }
}
