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
package io.gravitee.apim.rest.api.automation.resource;

import io.gravitee.apim.core.application_metadata.crud_service.ApplicationMetadataCrudService;
import io.gravitee.apim.core.application_metadata.query_service.ApplicationMetadataQueryService;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.mapper.ApplicationMapper;
import io.gravitee.apim.rest.api.automation.model.ApplicationSpec;
import io.gravitee.apim.rest.api.automation.model.Member;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationResource extends AbstractResource {

    @Inject
    private ApplicationService applicationService;

    @Inject
    private MembershipService membershipService;

    @Inject
    private UserService userService;

    @Inject
    private ApplicationMetadataQueryService applicationMetadataQueryService;

    @Inject
    private ApplicationMetadataCrudService applicationMetadataCrudService;

    @PathParam("hrid")
    private String hrid;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = { RolePermissionAction.READ }) })
    public Response getApplicationByHRID(@QueryParam("legacy") boolean legacy) {
        var executionContext = GraviteeContext.getExecutionContext();
        try {
            ApplicationEntity applicationEntity = applicationService.findById(
                executionContext,
                legacy ? hrid : IdBuilder.builder(executionContext, hrid).buildId()
            );

            ApplicationSpec applicationSpec = ApplicationMapper.INSTANCE.applicationEntityToApplicationSpec(applicationEntity);
            addMembers(executionContext, applicationEntity, applicationSpec);
            addMetadata(applicationEntity, applicationSpec);
            return Response
                .ok(
                    ApplicationMapper.INSTANCE.applicationSpecToApplicationState(
                        applicationSpec,
                        applicationEntity.getId(),
                        executionContext.getOrganizationId(),
                        executionContext.getEnvironmentId()
                    )
                )
                .build();
        } catch (ApplicationNotFoundException e) {
            throw new HRIDNotFoundException(hrid);
        }
    }

    private void addMembers(ExecutionContext executionContext, ApplicationEntity applicationEntity, ApplicationSpec applicationSpec) {
        Set<MemberEntity> memberEntities = membershipService.getMembersByReferenceAndRole(
            GraviteeContext.getExecutionContext(),
            MembershipReferenceType.APPLICATION,
            applicationEntity.getId(),
            null
        );

        Map<String, RoleEntity> membersAndRoles = new HashMap<>();
        memberEntities
            .stream()
            .filter(m -> !Objects.equals(m.getId(), applicationEntity.getPrimaryOwner().getId()))
            .filter(m -> Objects.equals(m.getType(), MembershipMemberType.USER))
            .forEach(m -> membersAndRoles.put(m.getId(), Optional.ofNullable(m.getRoles()).map(List::getFirst).orElse(null)));

        if (!membersAndRoles.isEmpty()) {
            var members = userService
                .findByIds(executionContext, membersAndRoles.keySet())
                .stream()
                .map(user -> {
                    Member member = new Member();
                    member.setSource(user.getSource());
                    member.setSourceId(user.getSourceId());
                    Optional.ofNullable(membersAndRoles.get(user.getId())).map(RoleEntity::getName).ifPresent(member::setRole);
                    return member;
                })
                .collect(Collectors.toSet());

            applicationSpec.setMembers(members);
        }
    }

    private void addMetadata(ApplicationEntity applicationEntity, ApplicationSpec applicationSpec) {
        var exitingAppMetadata = applicationMetadataQueryService.findAllByApplication(applicationEntity.getId());
        applicationSpec.setMetadata(ApplicationMapper.INSTANCE.metadataEntityToMetadata(exitingAppMetadata));
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.DELETE) })
    public Response deleteApplicationByHrid(@QueryParam("legacy") boolean legacy) {
        var executionContext = GraviteeContext.getExecutionContext();

        try {
            String applicationId = legacy ? hrid : IdBuilder.builder(executionContext, hrid).buildId();
            applicationService.archive(executionContext, applicationId);
            removeMetadata(applicationId);
        } catch (ApplicationNotFoundException e) {
            throw new HRIDNotFoundException(hrid);
        }
        return Response.noContent().build();
    }

    private void removeMetadata(String applicationId) {
        applicationMetadataQueryService.findAllByApplication(applicationId).forEach(applicationMetadataCrudService::delete);
    }
}
