/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.cockpit.command.handler;

import static io.gravitee.rest.api.service.cockpit.command.handler.UserCommandHandler.COCKPIT_SOURCE;

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandHandler;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.membership.MembershipCommand;
import io.gravitee.cockpit.api.command.membership.MembershipPayload;
import io.gravitee.cockpit.api.command.membership.MembershipReply;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import io.reactivex.Single;
import java.util.Collections;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MembershipCommandHandler implements CommandHandler<MembershipCommand, MembershipReply> {

    private final Logger logger = LoggerFactory.getLogger(MembershipCommandHandler.class);

    private final UserService userService;
    private final RoleService roleService;
    private final MembershipService membershipService;

    public MembershipCommandHandler(UserService userService, RoleService roleService, MembershipService membershipService) {
        this.userService = userService;
        this.roleService = roleService;
        this.membershipService = membershipService;
    }

    @Override
    public Command.Type handleType() {
        return Command.Type.MEMBERSHIP_COMMAND;
    }

    @Override
    public Single<MembershipReply> handle(MembershipCommand command) {
        MembershipPayload membershipPayload = command.getPayload();
        GraviteeContext.setCurrentOrganization(membershipPayload.getOrganizationId());

        try {
            RoleScope roleScope;
            MembershipReferenceType membershipReferenceType;

            try {
                roleScope = RoleScope.valueOf(membershipPayload.getReferenceType());
                membershipReferenceType = MembershipReferenceType.valueOf(membershipPayload.getReferenceType());
            } catch (Exception e) {
                logger.error("Invalid referenceType [{}].", membershipPayload.getReferenceType());
                return Single.just(new MembershipReply(command.getId(), CommandStatus.ERROR));
            }

            final UserEntity userEntity = userService.findBySource(COCKPIT_SOURCE, membershipPayload.getUserId(), false);
            final RoleEntity roleEntity = findRole(roleScope, membershipPayload.getRole());
            final MembershipService.MembershipReference membershipReference = new MembershipService.MembershipReference(
                membershipReferenceType,
                membershipPayload.getReferenceId()
            );
            final MembershipService.MembershipMember membershipMember = new MembershipService.MembershipMember(
                userEntity.getId(),
                null,
                MembershipMemberType.USER
            );
            final MembershipService.MembershipRole membershipRole = new MembershipService.MembershipRole(
                roleEntity.getScope(),
                roleEntity.getName()
            );

            membershipService.updateRolesToMemberOnReference(
                membershipReference,
                membershipMember,
                Collections.singletonList(membershipRole),
                COCKPIT_SOURCE,
                false
            );

            logger.info(
                "Role [{}] assigned on {} [{}] for user [{}] and organization [{}].",
                membershipPayload.getRole(),
                membershipPayload.getReferenceType(),
                membershipPayload.getReferenceId(),
                userEntity.getId(),
                membershipPayload.getOrganizationId()
            );
            return Single.just(new MembershipReply(command.getId(), CommandStatus.SUCCEEDED));
        } catch (Exception e) {
            logger.error(
                "Error occurred when trying to assign role [{}] on {} [{}] for cockpit user [{}] and organization [{}].",
                membershipPayload.getRole(),
                membershipPayload.getReferenceType(),
                membershipPayload.getReferenceId(),
                membershipPayload.getUserId(),
                membershipPayload.getOrganizationId(),
                e
            );
            return Single.just(new MembershipReply(command.getId(), CommandStatus.ERROR));
        } finally {
            GraviteeContext.cleanContext();
        }
    }

    private RoleEntity findRole(RoleScope roleScope, String roleName) {
        // Need to map cockpit role to apim role (ORGANIZATION_PRIMARY_OWNER | ORGANIZATION_OWNER -> ADMIN, ENVIRONMENT_PRIMARY_OWNER | ENVIRONMENT_OWNER -> ADMIN).
        final String mappedRoleName = roleName
            .replace(roleScope.name() + "_", "")
            .replace("PRIMARY_OWNER", "ADMIN")
            .replace("OWNER", "ADMIN");

        final Optional<RoleEntity> role = roleService.findByScopeAndName(roleScope, mappedRoleName);

        if (!role.isPresent()) {
            throw new RoleNotFoundException(roleScope, mappedRoleName);
        }

        return role.get();
    }
}
