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
package io.gravitee.rest.api.service.cockpit.command.handler;

import static io.gravitee.rest.api.service.cockpit.command.handler.UserCommandHandler.COCKPIT_SOURCE;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.DEFAULT_ROLE_ORGANIZATION_USER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_API_PUBLISHER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_FEDERATION_AGENT;

import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.targettoken.TargetTokenCommand;
import io.gravitee.cockpit.api.command.v1.targettoken.TargetTokenCommandPayload;
import io.gravitee.cockpit.api.command.v1.targettoken.TargetTokenReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.NewRoleEntity;
import io.gravitee.rest.api.model.NewTokenEntity;
import io.gravitee.rest.api.model.TokenEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.reactivex.rxjava3.core.Single;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TargetTokenCommandHandler implements CommandHandler<TargetTokenCommand, TargetTokenReply> {

    public static final String CLOUD_TOKEN_SOURCE = "cloud-token";
    public static final String TARGET_TOKEN_USER_NAME = "Cloud Token User";
    public static final String TARGET_TOKEN_NAME = "Cloud Target Token";

    private final UserService userService;
    private final MembershipService membershipService;
    private final RoleService roleService;
    private final TokenService tokenService;

    @Override
    public String supportType() {
        return CockpitCommandType.TARGET_TOKEN.name();
    }

    public Single<TargetTokenReply> handle(TargetTokenCommand command) {
        TargetTokenCommandPayload payload = command.getPayload();
        ExecutionContext context = new ExecutionContext(payload.organizationId(), payload.environmentId());
        UserEntity user = null;
        TokenEntity tokenEntity = null;

        try {
            user = createUser(context, command);
            if (user == null) {
                return Single.just(new TargetTokenReply(command.getId(), "Failed to create user."));
            }

            if (!assignOrganizationRole(context, payload, user)) {
                rollbackUserCreation(context, user);
                return Single.just(new TargetTokenReply(command.getId(), "Failed to assign organization role."));
            }

            if (!assignEnvironmentRole(context, payload, user)) {
                rollbackMemberships(context, user);
                rollbackUserCreation(context, user);
                return Single.just(new TargetTokenReply(command.getId(), "Failed to assign environment role."));
            }

            tokenEntity = createToken(context, user);
            return Single.just(new TargetTokenReply(command.getId(), CommandStatus.SUCCEEDED, tokenEntity.getToken()));
        } catch (Exception e) {
            log.error("Rolling back due to failure in handling target token command.", e);
            rollbackTokenCreation(context, tokenEntity);
            rollbackMemberships(context, user);
            rollbackUserCreation(context, user);

            String errorDetails = String.format(
                "Error occurred creating cloud target token for organization [%s] and environment [%s].",
                payload.organizationId(),
                payload.environmentId()
            );
            return Single.just(new TargetTokenReply(command.getId(), errorDetails));
        }
    }

    private UserEntity createUser(ExecutionContext context, TargetTokenCommand command) {
        NewExternalUserEntity newUser = new NewExternalUserEntity();
        newUser.setSourceId(command.getPayload().id());
        newUser.setSource(CLOUD_TOKEN_SOURCE);
        newUser.setLastname(TARGET_TOKEN_USER_NAME);

        try {
            UserEntity userEntity = userService.create(context, newUser, false);
            log.info("Cloud target token user created with id [{}].", userEntity.getId());
            return userEntity;
        } catch (Exception e) {
            log.error("Error occurred when creating cloud target token user.", e);
            return null;
        }
    }

    private boolean assignOrganizationRole(ExecutionContext context, TargetTokenCommandPayload payload, UserEntity user) {
        String roleName = payload.scope() == TargetTokenCommandPayload.Scope.GKO
            ? SystemRole.ADMIN.name()
            : DEFAULT_ROLE_ORGANIZATION_USER.getName();
        if (roleService.findByScopeAndName(RoleScope.ORGANIZATION, roleName, payload.organizationId()).isEmpty()) {
            log.error("Couldn't find {} role for organization with id [{}]", roleName, payload.organizationId());
            return false;
        }

        MembershipService.MembershipMember member = new MembershipService.MembershipMember(user.getId(), null, MembershipMemberType.USER);

        MembershipService.MembershipReference reference = new MembershipService.MembershipReference(
            MembershipReferenceType.ORGANIZATION,
            payload.organizationId()
        );
        MembershipService.MembershipRole role = new MembershipService.MembershipRole(RoleScope.ORGANIZATION, roleName);
        membershipService.updateRolesToMemberOnReference(
            context,
            reference,
            member,
            Collections.singletonList(role),
            COCKPIT_SOURCE,
            false
        );

        return true;
    }

    private boolean assignEnvironmentRole(ExecutionContext context, TargetTokenCommandPayload payload, UserEntity user) {
        NewRoleEntity newRole = payload.scope() == TargetTokenCommandPayload.Scope.FEDERATION
            ? ROLE_ENVIRONMENT_FEDERATION_AGENT
            : ROLE_ENVIRONMENT_API_PUBLISHER;

        if (roleService.findByScopeAndName(RoleScope.ENVIRONMENT, newRole.getName(), payload.organizationId()).isEmpty()) {
            log.error(
                "Couldn't find role {} with scope {} for organization with id [{}]",
                newRole.getName(),
                RoleScope.ENVIRONMENT,
                payload.organizationId()
            );
            return false;
        }

        MembershipService.MembershipMember member = new MembershipService.MembershipMember(user.getId(), null, MembershipMemberType.USER);
        MembershipService.MembershipReference reference = new MembershipService.MembershipReference(
            MembershipReferenceType.ENVIRONMENT,
            payload.environmentId()
        );
        MembershipService.MembershipRole role = new MembershipService.MembershipRole(RoleScope.ENVIRONMENT, newRole.getName());

        membershipService.updateRolesToMemberOnReference(
            context,
            reference,
            member,
            Collections.singletonList(role),
            COCKPIT_SOURCE,
            false
        );
        return true;
    }

    private TokenEntity createToken(ExecutionContext context, UserEntity user) {
        NewTokenEntity token = new NewTokenEntity();
        token.setName(TARGET_TOKEN_NAME);
        return tokenService.create(context, token, user.getId());
    }

    private void rollbackUserCreation(ExecutionContext context, UserEntity user) {
        if (user != null) {
            userService.delete(context, user.getId());
            log.info("Rolled back user creation with id [{}].", user.getId());
        }
    }

    private void rollbackMemberships(ExecutionContext context, UserEntity user) {
        if (user != null) {
            membershipService.removeMemberMemberships(context, MembershipMemberType.USER, user.getId());
            log.info("Rolled back memberships for user [{}].", user.getId());
        }
    }

    private void rollbackTokenCreation(ExecutionContext context, TokenEntity tokenEntity) {
        if (tokenEntity != null) {
            tokenService.revoke(context, tokenEntity.getId());
            log.info("Revoked token with id [{}].", tokenEntity.getId());
        }
    }
}
