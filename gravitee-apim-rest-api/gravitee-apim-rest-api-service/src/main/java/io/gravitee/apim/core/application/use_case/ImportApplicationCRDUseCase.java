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
import io.gravitee.apim.core.application.domain_service.ValidateApplicationCRDDomainService;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDSpec;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDStatus;
import io.gravitee.apim.core.application.model.crd.ApplicationMetadataCRD;
import io.gravitee.apim.core.application_metadata.crud_service.ApplicationMetadataCrudService;
import io.gravitee.apim.core.application_metadata.query_service.ApplicationMetadataQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.AbstractDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.member.domain_service.CRDMembersDomainService;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.Origin;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationMetadataEntity;
import io.gravitee.rest.api.model.UpdateApplicationMetadataEntity;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
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
    private final CRDMembersDomainService membersDomainService;
    private final ValidateApplicationCRDDomainService crdValidator;

    public ImportApplicationCRDUseCase(
        ApplicationCrudService applicationCrudService,
        ImportApplicationCRDDomainService importApplicationCRDDomainService,
        ApplicationMetadataCrudService applicationMetadataCrudService,
        ApplicationMetadataQueryService applicationMetadataQueryService,
        CRDMembersDomainService membersDomainService,
        ValidateApplicationCRDDomainService crdValidator
    ) {
        this.applicationCrudService = applicationCrudService;
        this.importApplicationCRDDomainService = importApplicationCRDDomainService;
        this.applicationMetadataCrudService = applicationMetadataCrudService;
        this.applicationMetadataQueryService = applicationMetadataQueryService;
        this.membersDomainService = membersDomainService;
        this.crdValidator = crdValidator;
    }

    public record Output(ApplicationCRDStatus status) {}

    public record Input(AuditInfo auditInfo, ApplicationCRDSpec crd) {}

    public Output execute(Input input) {
        try {
            var application = applicationCrudService.findById(input.crd.getId(), input.auditInfo.environmentId());
            return new Output(this.update(input, application));
        } catch (ApplicationNotFoundException e) {
            return new Output(this.create(input));
        }
    }

    private ApplicationCRDStatus create(Input input) {
        try {
            var validationResult = validateAndSanitize(input);

            var warnings = validationResult.warning().orElseGet(List::of);

            var sanitizedInput = validationResult.value().orElseThrow(() -> new ValidationDomainException("Unable to sanitize CRD spec"));

            var newApplicationEntity = sanitizedInput.crd().toNewApplicationEntity();
            var newApplication = importApplicationCRDDomainService.create(newApplicationEntity, sanitizedInput.auditInfo);

            createOrUpdateApplicationMetadata(sanitizedInput, newApplication);

            membersDomainService.updateApplicationMembers(
                sanitizedInput.auditInfo.organizationId(),
                newApplication.getId(),
                sanitizedInput.crd.getMembers()
            );

            return ApplicationCRDStatus
                .builder()
                .id(newApplication.getId())
                .organizationId(sanitizedInput.auditInfo.organizationId())
                .environmentId(sanitizedInput.auditInfo.environmentId())
                .errors(ApplicationCRDStatus.Errors.fromErrorList(warnings))
                .build();
        } catch (AbstractDomainException e) {
            throw e;
        } catch (Exception e) {
            throw new TechnicalManagementException(e);
        }
    }

    private ApplicationCRDStatus update(Input input, BaseApplicationEntity application) {
        try {
            var validationResult = validateAndSanitize(input);

            var warnings = validationResult.warning().orElseGet(List::of);

            var sanitizedInput = validationResult.value().orElseThrow(() -> new ValidationDomainException("Unable to sanitize CRD spec"));

            var updateApplicationEntity = sanitizedInput.crd.toUpdateApplicationEntity();

            var updatedApplication = importApplicationCRDDomainService.update(
                application.getId(),
                updateApplicationEntity,
                sanitizedInput.auditInfo
            );

            createOrUpdateApplicationMetadata(sanitizedInput, updatedApplication);

            membersDomainService.updateApplicationMembers(
                sanitizedInput.auditInfo.organizationId(),
                updatedApplication.getId(),
                sanitizedInput.crd.getMembers()
            );

            return ApplicationCRDStatus
                .builder()
                .id(updatedApplication.getId())
                .organizationId(input.auditInfo.organizationId())
                .environmentId(input.auditInfo.environmentId())
                .errors(ApplicationCRDStatus.Errors.fromErrorList(warnings))
                .build();
        } catch (AbstractDomainException e) {
            throw e;
        } catch (Exception e) {
            throw new TechnicalManagementException(e);
        }
    }

    private Validator.Result<ImportApplicationCRDUseCase.Input> validateAndSanitize(Input input) {
        var validationResult = crdValidator
            .validateAndSanitize(new ValidateApplicationCRDDomainService.Input(input.auditInfo(), input.crd()))
            .map(sanitized -> new ImportApplicationCRDUseCase.Input(sanitized.auditInfo(), sanitized.spec()));

        validationResult
            .severe()
            .ifPresent(errors -> {
                throw new ValidationDomainException(
                    String.format(
                        "Unable to import because of errors [%s]",
                        String.join(",", errors.stream().map(Validator.Error::getMessage).toList())
                    )
                );
            });

        return validationResult;
    }

    private void createOrUpdateApplicationMetadata(Input input, BaseApplicationEntity application) {
        if (!CollectionUtils.isEmpty(input.crd.getMetadata())) {
            var applicationId = application.getId();
            var exitingAppMetadata = applicationMetadataQueryService.findAllByApplication(applicationId);
            for (ApplicationMetadataCRD metadata : input.crd.getMetadata()) {
                metadata.setApplicationId(applicationId);

                var applicationMetadataEntity = exitingAppMetadata
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
        var newApplicationMetadataEntity = new NewApplicationMetadataEntity();
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
        var updateApplicationMetadataEntity = new UpdateApplicationMetadataEntity();
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
