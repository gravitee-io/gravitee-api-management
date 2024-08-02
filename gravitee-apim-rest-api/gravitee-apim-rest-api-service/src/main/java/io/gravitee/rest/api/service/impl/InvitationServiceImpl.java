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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.GROUP_INVITATION;
import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.REGISTRATION_PATH;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.model.Invitation;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.MembershipService.MembershipReference;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.InvitationEmailAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.InvitationNotFoundException;
import io.gravitee.rest.api.service.exceptions.MemberEmailAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class InvitationServiceImpl extends TransactionalService implements InvitationService {

    private final Logger LOGGER = LoggerFactory.getLogger(InvitationServiceImpl.class);

    @Lazy
    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private GroupService groupService;

    @Autowired
    PermissionService permissionService;

    @Override
    public InvitationEntity create(final ExecutionContext executionContext, final NewInvitationEntity invitation) {
        final List<InvitationEntity> invitations = findByReference(invitation.getReferenceType(), invitation.getReferenceId());
        if (invitations.stream().map(InvitationEntity::getEmail).anyMatch(invitation.getEmail()::equals)) {
            throw new InvitationEmailAlreadyExistsException(invitation.getEmail());
        }
        try {
            // First check if user exists
            final Optional<UserEntity> existingUser = userService.findByEmail(executionContext, invitation.getEmail());
            if (existingUser.isPresent()) {
                final UserEntity user = existingUser.get();

                Set<String> groupUsers = membershipService
                    .getMembershipsByReference(MembershipReferenceType.GROUP, invitation.getReferenceId())
                    .stream()
                    .map(MembershipEntity::getMemberId)
                    .collect(Collectors.toSet());
                final GroupEntity group = groupService.findById(executionContext, invitation.getReferenceId());
                if (groupUsers.contains(user.getId())) {
                    throw new MemberEmailAlreadyExistsException(invitation.getEmail());
                }

                // override permission if not allowed
                final boolean hasPermission = permissionService.hasPermission(
                    executionContext,
                    RolePermission.ENVIRONMENT_GROUP,
                    executionContext.getEnvironmentId(),
                    CREATE,
                    UPDATE,
                    DELETE
                );
                if (!hasPermission && group.isLockApiRole()) {
                    invitation.setApiRole(null);
                }
                if (!hasPermission && group.isLockApplicationRole()) {
                    invitation.setApplicationRole(null);
                }

                addMember(
                    executionContext,
                    invitation.getReferenceType().name(),
                    invitation.getReferenceId(),
                    user.getId(),
                    invitation.getApiRole(),
                    invitation.getApplicationRole()
                );
                return null;
            } else {
                sendGroupInvitationEmail(executionContext, invitation);
                final Invitation createdInvitation = invitationRepository.create(convert(invitation));
                return convert(createdInvitation);
            }
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to create invitation for email " + invitation.getEmail();
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public InvitationEntity update(final UpdateInvitationEntity invitation) {
        try {
            final Optional<Invitation> invitationOptional = invitationRepository.findById(invitation.getId());
            if (invitationOptional.isPresent()) {
                final Invitation invitationToUpdate = invitationOptional.get();
                if (!invitationToUpdate.getReferenceId().equals(invitation.getReferenceId())) {
                    throw new InvitationNotFoundException(invitation.getId());
                }

                invitationToUpdate.setApiRole(invitation.getApiRole());
                invitationToUpdate.setApplicationRole(invitation.getApplicationRole());
                invitationToUpdate.setUpdatedAt(new Date());
                return convert(invitationRepository.update(invitationToUpdate));
            } else {
                throw new InvitationNotFoundException(invitation.getId());
            }
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to update invitation with email " + invitation.getEmail();
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public void addMember(
        final ExecutionContext executionContext,
        final String referenceType,
        final String referenceId,
        final String userId,
        final String apiRole,
        final String applicationRole
    ) {
        addOrUpdateMemberByScope(executionContext, referenceType, referenceId, userId, RoleScope.API, apiRole);
        addOrUpdateMemberByScope(executionContext, referenceType, referenceId, userId, RoleScope.APPLICATION, applicationRole);
        addOrUpdateMemberByScope(executionContext, referenceType, referenceId, userId, RoleScope.GROUP, null);
    }

    private void addOrUpdateMemberByScope(
        final ExecutionContext executionContext,
        final String referenceType,
        final String referenceId,
        final String userId,
        final RoleScope roleScope,
        final String defaultRole
    ) {
        String defaultRoleName = null;
        if (defaultRole == null) {
            final List<RoleEntity> defaultRoles = roleService.findDefaultRoleByScopes(executionContext.getOrganizationId(), roleScope);
            if (defaultRoles != null && !defaultRoles.isEmpty()) {
                defaultRoleName = defaultRoles.get(0).getName();
            }
        } else {
            defaultRoleName = defaultRole;
        }
        if (defaultRoleName != null) {
            membershipService.addRoleToMemberOnReference(
                executionContext,
                new MembershipReference(MembershipReferenceType.valueOf(referenceType), referenceId),
                new MembershipService.MembershipMember(userId, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(roleScope, defaultRoleName)
            );
        }
    }

    private void sendGroupInvitationEmail(final ExecutionContext executionContext, NewInvitationEntity invitation) {
        final UserEntity userEntity = new UserEntity();
        userEntity.setEmail(invitation.getEmail());
        final GroupEntity group = groupService.findById(executionContext, invitation.getReferenceId());
        emailService.sendEmailNotification(
            executionContext,
            new EmailNotificationBuilder()
                .to(invitation.getEmail())
                .template(EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_USER_GROUP_INVITATION)
                .params(userService.getTokenRegistrationParams(executionContext, userEntity, REGISTRATION_PATH, GROUP_INVITATION))
                .param("group", group)
                .build()
        );
    }

    @Override
    public List<InvitationEntity> findAll() {
        try {
            final Set<Invitation> invitations = invitationRepository.findAll();
            return invitations.stream().map(this::convert).collect(toList());
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to list all invitations";
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public List<InvitationEntity> findByReference(final InvitationReferenceType referenceType, final String referenceId) {
        try {
            final List<Invitation> invitations = invitationRepository.findByReferenceIdAndReferenceType(
                referenceId,
                io.gravitee.repository.management.model.InvitationReferenceType.valueOf(referenceType.name())
            );
            return invitations.stream().map(this::convert).sorted(comparing(InvitationEntity::getEmail)).collect(toList());
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to list invitations by reference " + referenceType + '/' + referenceId;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public void delete(final String invitationId, final String referenceId) {
        try {
            final Optional<Invitation> optionalInvitation = invitationRepository.findById(invitationId);
            if (!optionalInvitation.isPresent() || !optionalInvitation.get().getReferenceId().equals(referenceId)) {
                throw new InvitationNotFoundException(invitationId);
            }
            invitationRepository.delete(invitationId);
        } catch (TechnicalException te) {
            final String msg = "An error occurs while trying to delete the invitation " + invitationId;
            LOGGER.error(msg, te);
            throw new TechnicalManagementException(msg, te);
        }
    }

    private Invitation convert(final NewInvitationEntity invitationEntity) {
        final Invitation invitation = new Invitation();
        invitation.setId(UuidString.generateRandom());
        invitation.setReferenceId(invitationEntity.getReferenceId());
        invitation.setReferenceType(invitationEntity.getReferenceType().name());
        invitation.setApiRole(invitationEntity.getApiRole());
        invitation.setApplicationRole(invitationEntity.getApplicationRole());
        invitation.setEmail(invitationEntity.getEmail());
        final Date now = new Date();
        invitation.setCreatedAt(now);
        return invitation;
    }

    private InvitationEntity convert(final Invitation invitation) {
        final InvitationEntity invitationEntity = new InvitationEntity();
        invitationEntity.setId(invitation.getId());
        invitationEntity.setReferenceId(invitation.getReferenceId());
        invitationEntity.setReferenceType(InvitationReferenceType.valueOf(invitation.getReferenceType()));
        invitationEntity.setApiRole(invitation.getApiRole());
        invitationEntity.setApplicationRole(invitation.getApplicationRole());
        invitationEntity.setEmail(invitation.getEmail());
        invitationEntity.setCreatedAt(invitation.getCreatedAt());
        return invitationEntity;
    }
}
