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

import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.membership.MembershipCommand;
import io.gravitee.cockpit.api.command.v1.membership.MembershipCommandPayload;
import io.gravitee.cockpit.api.command.v1.membership.MembershipReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import io.reactivex.rxjava3.core.Single;
import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MembershipCommandHandler implements CommandHandler<MembershipCommand, MembershipReply> {

    private final UserService userService;
    private final RoleService roleService;
    private final MembershipService membershipService;

    @Override
    public String supportType() {
        return CockpitCommandType.MEMBERSHIP.name();
    }

    @Override
    public Single<MembershipReply> handle(MembershipCommand command) {
        MembershipCommandPayload membershipPayload = command.getPayload();

        try {
            RoleScope roleScope;
            MembershipReferenceType membershipReferenceType;

            try {
                roleScope = RoleScope.valueOf(membershipPayload.referenceType());
                membershipReferenceType = MembershipReferenceType.valueOf(membershipPayload.referenceType());
            } catch (Exception e) {
                String errorDetails = "Invalid referenceType [%s].".formatted(membershipPayload.referenceType());
                log.error(errorDetails, e);
                return Single.just(new MembershipReply(command.getId(), errorDetails));
            }

            ExecutionContext executionContext = new ExecutionContext(
                membershipPayload.organizationId(),
                membershipReferenceType.equals(MembershipReferenceType.ENVIRONMENT) ? membershipPayload.referenceId() : null
            );

            final UserEntity userEntity = userService.findBySource(
                executionContext.getOrganizationId(),
                COCKPIT_SOURCE,
                membershipPayload.userId(),
                false
            );
            final RoleEntity roleEntity = findRole(executionContext.getOrganizationId(), roleScope, membershipPayload.role());
            final MembershipService.MembershipReference membershipReference = new MembershipService.MembershipReference(
                membershipReferenceType,
                membershipPayload.referenceId()
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
                executionContext,
                membershipReference,
                membershipMember,
                Collections.singletonList(membershipRole),
                COCKPIT_SOURCE,
                false
            );

            log.info(
                "Role [{}] assigned on {} [{}] for user [{}] and organization [{}].",
                membershipPayload.role(),
                membershipPayload.referenceType(),
                membershipPayload.referenceId(),
                userEntity.getId(),
                membershipPayload.organizationId()
            );
            return Single.just(new MembershipReply(command.getId()));
        } catch (Exception e) {
            String errorDetails =
                "Error occurred when trying to assign role [%s] on %s [%s] for cockpit user [%s] and organization [%s].".formatted(
                        membershipPayload.role(),
                        membershipPayload.referenceType(),
                        membershipPayload.referenceId(),
                        membershipPayload.userId(),
                        membershipPayload.organizationId()
                    );
            log.error(errorDetails, e);
            return Single.just(new MembershipReply(command.getId(), errorDetails));
        }
    }

    private RoleEntity findRole(String organizationId, RoleScope roleScope, String roleName) {
        // Need to map cockpit role to apim role (ORGANIZATION_PRIMARY_OWNER | ORGANIZATION_OWNER -> ADMIN, ENVIRONMENT_PRIMARY_OWNER | ENVIRONMENT_OWNER -> ADMIN).
        final String mappedRoleName = roleName
            .replace(roleScope.name() + "_", "")
            .replace("PRIMARY_OWNER", "ADMIN")
            .replace("OWNER", "ADMIN");

        final Optional<RoleEntity> role = roleService.findByScopeAndName(roleScope, mappedRoleName, organizationId);

        if (role.isEmpty()) {
            throw new RoleNotFoundException(roleScope, mappedRoleName);
        }

        return role.get();
    }
}
