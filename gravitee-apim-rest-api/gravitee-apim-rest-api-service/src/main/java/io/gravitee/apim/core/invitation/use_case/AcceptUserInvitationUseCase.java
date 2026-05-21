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
package io.gravitee.apim.core.invitation.use_case;

import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.OrganizationAuditLogEntity;
import io.gravitee.apim.core.audit.model.event.UserAuditEvent;
import io.gravitee.apim.core.invitation.crud_service.InvitationCrudService;
import io.gravitee.apim.core.invitation.domain_service.AcceptInvitationDomainService;
import io.gravitee.apim.core.invitation.exception.InvitationCanceledException;
import io.gravitee.apim.core.invitation.model.Invitation;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.domain_service.CreateUserDomainService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.user.model.RawPassword;
import io.gravitee.apim.core.user.service_provider.UserPasswordService;
import io.gravitee.apim.core.user.service_provider.UserPortalNotificationService;
import io.gravitee.apim.core.user.service_provider.UserRegistrationEnabledService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.UserAlreadyFinalizedException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AcceptUserInvitationUseCase {

    private final UserCrudService userCrudService;
    private final CreateUserDomainService createUserDomainService;
    private final InvitationCrudService invitationCrudService;
    private final AcceptInvitationDomainService acceptInvitationDomainService;
    private final UserRegistrationEnabledService userRegistrationEnabledService;
    private final UserPasswordService userPasswordService;
    private final UserPortalNotificationService userPortalNotificationService;
    private final AuditDomainService auditDomainService;

    public Output execute(Input input) {
        input.password().ifPresent(userPasswordService::validate);

        var pendingInvitations = switch (input.action()) {
            case UserRegistrationAction ignored -> {
                userRegistrationEnabledService.checkEnabled(input.executionContext());
                yield List.<Invitation>of();
            }
            case GroupInvitationAction a -> {
                var invitations = invitationCrudService.findByEmail(a.email());
                if (invitations.isEmpty()) {
                    throw new InvitationCanceledException(a.email());
                }
                yield invitations;
            }
        };

        var user = input
            .action()
            .existingUserId()
            .map(userId -> findAndValidateExistingUser(userId, input.executionContext()))
            .orElseGet(() ->
                createUserDomainService.createExternalUser(
                    input.executionContext(),
                    input.action().email(),
                    input.firstname(),
                    input.lastname()
                )
            );

        switch (input.action()) {
            case UserRegistrationAction ignored -> {}
            case GroupInvitationAction ignored -> pendingInvitations.forEach(invitation ->
                processInvitation(input.executionContext(), invitation, user.getId())
            );
        }

        user.setUpdatedAt(Date.from(TimeProvider.now().toInstant()));

        var updated = input
            .password()
            .map(pwd -> userCrudService.updateAndSetPassword(user, userPasswordService.encode(pwd)))
            .orElseGet(() -> userCrudService.update(user));

        auditDomainService.createOrganizationAuditLog(
            OrganizationAuditLogEntity.builder()
                .organizationId(input.executionContext().getOrganizationId())
                .actor(AuditActor.builder().userId(updated.getId()).build())
                .properties(Map.of(AuditProperties.USER, updated.getId()))
                .event(UserAuditEvent.USER_CREATED)
                .createdAt(TimeProvider.now())
                .oldValue(null)
                .newValue(updated)
                .build()
        );

        userPortalNotificationService.triggerUserRegistered(input.executionContext(), updated);

        return new Output(updated);
    }

    private BaseUserEntity findAndValidateExistingUser(String userId, ExecutionContext executionContext) {
        var user = userCrudService.findBaseUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (userCrudService.isPasswordSet(userId)) {
            throw new UserAlreadyFinalizedException(executionContext.getOrganizationId());
        }
        return user;
    }

    private void processInvitation(ExecutionContext executionContext, Invitation invitation, String userId) {
        acceptInvitationDomainService.addMember(executionContext, invitation, userId);
        invitationCrudService.delete(invitation.id());
    }

    public sealed interface Action permits UserRegistrationAction, GroupInvitationAction {
        String email();

        Optional<String> existingUserId();
    }

    public record UserRegistrationAction(String email, Optional<String> existingUserId) implements Action {}

    public record GroupInvitationAction(String email, Optional<String> existingUserId) implements Action {}

    public record Input(
        ExecutionContext executionContext,
        Action action,
        Optional<RawPassword> password,
        Optional<String> firstname,
        Optional<String> lastname
    ) {}

    public record Output(BaseUserEntity user) {}
}
