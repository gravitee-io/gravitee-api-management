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
package io.gravitee.apim.core.application.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.application.domain_service.ImportApplicationCRDDomainService;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDSpec;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDStatus;
import io.gravitee.apim.core.application.model.crd.ApplicationMetadataCRD;
import io.gravitee.apim.core.application_metadata.crud_service.ApplicationMetadataCrudService;
import io.gravitee.apim.core.application_metadata.query_service.ApplicationMetadataQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.AbstractDomainException;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.definition.model.Origin;
import io.gravitee.rest.api.model.ApplicationMetadataEntity;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationMetadataEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationMetadataEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@Slf4j
public class ImportApplicationCRDUseCase {

    private final ApplicationCrudService applicationCrudService;
    private final ImportApplicationCRDDomainService importApplicationCRDDomainService;
    private final ApplicationMetadataCrudService applicationMetadataCrudService;
    private final ApplicationMetadataQueryService applicationMetadataQueryService;

    public ImportApplicationCRDUseCase(
        ApplicationCrudService applicationCrudService,
        ImportApplicationCRDDomainService importApplicationCRDDomainService,
        ApplicationMetadataCrudService applicationMetadataCrudService,
        ApplicationMetadataQueryService applicationMetadataQueryService
    ) {
        this.applicationCrudService = applicationCrudService;
        this.importApplicationCRDDomainService = importApplicationCRDDomainService;
        this.applicationMetadataCrudService = applicationMetadataCrudService;
        this.applicationMetadataQueryService = applicationMetadataQueryService;
    }

    public record Output(ApplicationCRDStatus status) {}

    public record Input(AuditInfo auditInfo, ApplicationCRDSpec crd) {}

    public Output execute(Input input) {
        if (input.crd.getId() == null) {
            return new Output(this.create(input));
        }

        var application = applicationCrudService.findById(input.crd.getId(), input.auditInfo.environmentId());
        return new Output(this.update(input, application));
    }

    private ApplicationCRDStatus create(Input input) {
        try {
            NewApplicationEntity newApplicationEntity = input.crd.toNewApplicationEntity();

            BaseApplicationEntity newApplication = importApplicationCRDDomainService.create(newApplicationEntity, input.auditInfo);

            createOrUpdateApplicationMetadata(input, newApplication);

            var ec = GraviteeContext.getExecutionContext();
            return ApplicationCRDStatus
                .builder()
                .id(newApplication.getId())
                .organizationId(ec.getOrganizationId())
                .environmentId(ec.getEnvironmentId())
                .build();
        } catch (AbstractDomainException e) {
            throw e;
        } catch (Exception e) {
            throw new TechnicalManagementException(e);
        }
    }

    private ApplicationCRDStatus update(Input input, BaseApplicationEntity application) {
        try {
            UpdateApplicationEntity updateApplicationEntity = input.crd.toUpdateApplicationEntity();

            BaseApplicationEntity updatedApplication = importApplicationCRDDomainService.update(
                application.getId(),
                updateApplicationEntity,
                input.auditInfo
            );

            createOrUpdateApplicationMetadata(input, updatedApplication);

            var ec = GraviteeContext.getExecutionContext();
            return ApplicationCRDStatus
                .builder()
                .id(updatedApplication.getId())
                .organizationId(ec.getOrganizationId())
                .environmentId(ec.getEnvironmentId())
                .build();
        } catch (AbstractDomainException e) {
            throw e;
        } catch (Exception e) {
            throw new TechnicalManagementException(e);
        }
    }

    private void createOrUpdateApplicationMetadata(Input input, BaseApplicationEntity application) {
        if (!CollectionUtils.isEmpty(input.crd.getMetadata())) {
            String applicationId = application.getId();
            List<ApplicationMetadataEntity> exitingAppMetadata = applicationMetadataQueryService.findAllByApplication(applicationId);
            for (ApplicationMetadataCRD metadata : input.crd.getMetadata()) {
                metadata.setApplicationId(applicationId);

                Optional<ApplicationMetadataEntity> applicationMetadataEntity = exitingAppMetadata
                    .stream()
                    .filter(ame -> ame.getName().equals(metadata.getName()))
                    .findFirst();

                if (applicationMetadataEntity.isPresent()) {
                    exitingAppMetadata.remove(applicationMetadataEntity.get());
                    applicationMetadataCrudService.update(toUpdateApplicationMetadataEntity(metadata));
                } else {
                    applicationMetadataCrudService.create(toNewApplicationMetadataEntity(metadata));
                }
            }

            // Get rid of deleted metadata
            exitingAppMetadata.forEach(applicationMetadataCrudService::delete);
        }
    }

    private NewApplicationMetadataEntity toNewApplicationMetadataEntity(ApplicationMetadataCRD crd) {
        NewApplicationMetadataEntity newApplicationMetadataEntity = new NewApplicationMetadataEntity();
        newApplicationMetadataEntity.setApplicationId(crd.getApplicationId());
        newApplicationMetadataEntity.setName(crd.getName());
        newApplicationMetadataEntity.setValue(crd.getValue());
        newApplicationMetadataEntity.setFormat(crd.getFormat());
        newApplicationMetadataEntity.setDefaultValue(crd.getDefaultValue());
        newApplicationMetadataEntity.setOrigin(Origin.KUBERNETES);
        newApplicationMetadataEntity.setHidden(crd.isHidden());

        return newApplicationMetadataEntity;
    }

    private UpdateApplicationMetadataEntity toUpdateApplicationMetadataEntity(ApplicationMetadataCRD crd) {
        UpdateApplicationMetadataEntity updateApplicationMetadataEntity = new UpdateApplicationMetadataEntity();
        updateApplicationMetadataEntity.setApplicationId(crd.getApplicationId());
        updateApplicationMetadataEntity.setName(crd.getName());
        updateApplicationMetadataEntity.setValue(crd.getValue());
        updateApplicationMetadataEntity.setFormat(crd.getFormat());
        updateApplicationMetadataEntity.setDefaultValue(crd.getDefaultValue());
        updateApplicationMetadataEntity.setHidden(crd.isHidden());
        updateApplicationMetadataEntity.setKey(crd.getKey());

        return updateApplicationMetadataEntity;
    }
}
