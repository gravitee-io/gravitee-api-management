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
package io.gravitee.apim.core.application_member.use_case;

import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.query_service.MemberQueryService;
import io.gravitee.apim.core.membership.domain_service.MembershipDomainService;
import io.gravitee.apim.core.membership.exception.RoleNotFoundException;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.exceptions.MembershipAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AddApplicationMemberUseCase {

    private final MembershipDomainService membershipDomainService;
    private final RoleQueryService roleQueryService;
    private final MemberQueryService memberQueryService;

    public record Input(
        String applicationId,
        List<AddMemberRequest> members,
        boolean sendNotification,
        String environmentId,
        String organizationId
    ) {}

    public record AddMemberRequest(String userId, String reference, String roleName) {}

    public record Output(List<Member> createdMembers) {}

    public Output execute(Input input) {
        var executionContext = new ExecutionContext(input.organizationId(), input.environmentId());
        var existingMemberIds = getExistingMemberIds(input.applicationId());

        var createdMembers = input
            .members()
            .stream()
            .map(member -> createMember(input, executionContext, member, existingMemberIds))
            .toList();

        return new Output(createdMembers);
    }

    private Member createMember(
        Input input,
        ExecutionContext executionContext,
        AddMemberRequest addMemberRequest,
        Set<String> existingMemberIds
    ) {
        validateRole(addMemberRequest.roleName(), input.organizationId());
        validateMembershipDoesNotAlreadyExist(input.applicationId(), addMemberRequest.userId(), existingMemberIds);

        var createdMembership = membershipDomainService.createNewMembership(
            executionContext,
            MembershipReferenceType.APPLICATION,
            input.applicationId(),
            addMemberRequest.userId(),
            addMemberRequest.reference(),
            addMemberRequest.roleName()
        );

        if (addMemberRequest.userId() != null) {
            existingMemberIds.add(addMemberRequest.userId());
        }

        return mapCreatedMembership(input.applicationId(), addMemberRequest.userId(), addMemberRequest.roleName(), createdMembership);
    }

    private void validateRole(String roleName, String organizationId) {
        if (PRIMARY_OWNER.name().equals(roleName)) {
            throw new SinglePrimaryOwnerException(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION);
        }

        var roleContext = ReferenceContext.builder().referenceType(ReferenceContext.Type.ORGANIZATION).referenceId(organizationId).build();
        roleQueryService.findApplicationRole(roleName, roleContext).orElseThrow(() -> new RoleNotFoundException(roleName, roleContext));
    }

    private void validateMembershipDoesNotAlreadyExist(String applicationId, String userId, Set<String> existingMemberIds) {
        if (userId != null && existingMemberIds.contains(userId)) {
            throw new MembershipAlreadyExistsException(
                userId,
                MembershipMemberType.USER,
                applicationId,
                io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION
            );
        }
    }

    private Member mapCreatedMembership(String applicationId, String userId, String roleName, MemberEntity createdMembership) {
        if (userId != null) {
            var member = memberQueryService.getUserMember(MembershipReferenceType.APPLICATION, applicationId, userId);
            if (member != null) {
                return member;
            }
        }

        if (createdMembership != null) {
            return Member.builder()
                .id(createdMembership.getId())
                .displayName(createdMembership.getDisplayName())
                .email(createdMembership.getEmail())
                .type(createdMembership.getType())
                .referenceType(createdMembership.getReferenceType())
                .referenceId(createdMembership.getReferenceId())
                .permissions(createdMembership.getPermissions())
                .createdAt(createdMembership.getCreatedAt())
                .updatedAt(createdMembership.getUpdatedAt())
                .roles(
                    createdMembership.getRoles() == null
                        ? List.of()
                        : createdMembership
                            .getRoles()
                            .stream()
                            .map(role ->
                                Member.Role.builder()
                                    .id(role.getId())
                                    .name(role.getName())
                                    .description(role.getDescription())
                                    .scope(role.getScope() == null ? RoleScope.APPLICATION : role.getScope())
                                    .defaultRole(role.isDefaultRole())
                                    .system(role.isSystem())
                                    .permissions(role.getPermissions())
                                    .build()
                            )
                            .toList()
                )
                .build();
        }

        return Member.builder()
            .id(userId)
            .type(MembershipMemberType.USER)
            .referenceType(io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION)
            .referenceId(applicationId)
            .roles(List.of(Member.Role.builder().name(roleName).scope(RoleScope.APPLICATION).build()))
            .build();
    }

    private Set<String> getExistingMemberIds(String applicationId) {
        return memberQueryService
            .getMembersByReference(MembershipReferenceType.APPLICATION, applicationId)
            .stream()
            .map(Member::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    }
}
