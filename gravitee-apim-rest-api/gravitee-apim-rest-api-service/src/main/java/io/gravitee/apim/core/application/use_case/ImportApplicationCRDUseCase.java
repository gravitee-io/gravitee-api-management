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

import static io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION;
import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;

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
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.member.model.MembershipMember;
import io.gravitee.apim.core.member.model.MembershipMemberType;
import io.gravitee.apim.core.member.model.MembershipReference;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.model.MembershipRole;
import io.gravitee.apim.core.member.model.RoleScope;
import io.gravitee.apim.core.member.query_service.MemberQueryService;
import io.gravitee.apim.core.user.domain_service.UserDomainService;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.definition.model.Origin;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationMetadataEntity;
import io.gravitee.rest.api.model.UpdateApplicationMetadataEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
    private final MemberQueryService memberQueryService;
    private final UserDomainService userDomainService;

    public ImportApplicationCRDUseCase(
        ApplicationCrudService applicationCrudService,
        ImportApplicationCRDDomainService importApplicationCRDDomainService,
        ApplicationMetadataCrudService applicationMetadataCrudService,
        ApplicationMetadataQueryService applicationMetadataQueryService,
        MemberQueryService memberQueryService,
        UserDomainService userDomainService
    ) {
        this.applicationCrudService = applicationCrudService;
        this.importApplicationCRDDomainService = importApplicationCRDDomainService;
        this.applicationMetadataCrudService = applicationMetadataCrudService;
        this.applicationMetadataQueryService = applicationMetadataQueryService;
        this.memberQueryService = memberQueryService;
        this.userDomainService = userDomainService;
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
            var newApplicationEntity = input.crd.toNewApplicationEntity();
            var newApplication = importApplicationCRDDomainService.create(newApplicationEntity, input.auditInfo);

            createOrUpdateApplicationMetadata(input, newApplication);

            var crdMemberIds = addUpdateApplicationMembers(input, newApplication);
            deleteOrphanApplicationMembers(newApplication.getId(), crdMemberIds);

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
            var updateApplicationEntity = input.crd.toUpdateApplicationEntity();
            var updatedApplication = importApplicationCRDDomainService.update(
                application.getId(),
                updateApplicationEntity,
                input.auditInfo
            );

            createOrUpdateApplicationMetadata(input, updatedApplication);
            addUpdateApplicationMembers(input, updatedApplication);

            var crdMemberIds = addUpdateApplicationMembers(input, updatedApplication);
            deleteOrphanApplicationMembers(updatedApplication.getId(), crdMemberIds);

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

    private ArrayList<String> addUpdateApplicationMembers(Input input, BaseApplicationEntity application) {
        var members = new ArrayList<String>();
        if (!CollectionUtils.isEmpty(input.crd.getMembers())) {
            input.crd
                .getMembers()
                .forEach(crdMember -> {
                    if (PRIMARY_OWNER.name().equals(crdMember.getRole())) {
                        throw new SinglePrimaryOwnerException(APPLICATION);
                    }

                    var optionalBaseUser = userDomainService.findBySource(
                        input.auditInfo.organizationId(),
                        crdMember.getSource(),
                        crdMember.getSourceId()
                    );
                    if (optionalBaseUser.isEmpty()) {
                        log.warn(
                            "member with source [{}] and source id [{}] doesn't exist. It will not be imported ...",
                            crdMember.getSource(),
                            crdMember.getSourceId()
                        );
                        return;
                    }

                    var user = optionalBaseUser.get();
                    var reference = new MembershipReference(MembershipReferenceType.APPLICATION, application.getId());
                    var member = new MembershipMember(user.getId(), crdMember.getReference(), MembershipMemberType.USER);
                    var role = new MembershipRole(RoleScope.APPLICATION, crdMember.getRole());

                    var userMember = memberQueryService.getUserMember(
                        MembershipReferenceType.APPLICATION,
                        application.getId(),
                        user.getId()
                    );
                    if (userMember != null) {
                        memberQueryService.updateRoleToMemberOnReference(reference, member, role);
                    } else {
                        memberQueryService.addRoleToMemberOnReference(reference, member, role);
                    }

                    members.add(user.getId());
                });
        }

        return members;
    }

    private void deleteOrphanApplicationMembers(String applicationId, List<String> crdMemberIds) {
        var existingApplicationMemberIds = memberQueryService
            .getMembersByReference(MembershipReferenceType.APPLICATION, applicationId)
            .stream()
            .filter(member -> {
                for (Member.Role role : member.getRoles()) {
                    if (role.isPrimaryOwner()) {
                        return false;
                    }
                }
                return true;
            })
            .map(Member::getId)
            .collect(Collectors.toList());

        crdMemberIds.forEach(existingApplicationMemberIds::remove);

        // delete all un-linked members
        existingApplicationMemberIds.forEach(memberId ->
            memberQueryService.deleteReferenceMember(
                MembershipReferenceType.APPLICATION,
                applicationId,
                MembershipMemberType.USER,
                memberId
            )
        );
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
