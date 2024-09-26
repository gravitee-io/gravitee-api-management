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
import io.gravitee.cockpit.api.command.v1.membership.DeleteMembershipCommand;
import io.gravitee.cockpit.api.command.v1.membership.DeleteMembershipCommandPayload;
import io.gravitee.cockpit.api.command.v1.membership.DeleteMembershipReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteMembershipCommandHandler implements CommandHandler<DeleteMembershipCommand, DeleteMembershipReply> {

    private final MembershipService membershipService;

    private final UserService userService;

    @Override
    public String supportType() {
        return CockpitCommandType.DELETE_MEMBERSHIP.name();
    }

    @Override
    public Single<DeleteMembershipReply> handle(DeleteMembershipCommand command) {
        DeleteMembershipCommandPayload membershipPayload = command.getPayload();
        try {
            MembershipReferenceType membershipReferenceType;
            try {
                membershipReferenceType = MembershipReferenceType.valueOf(membershipPayload.referenceType());
            } catch (Exception e) {
                String errorDetails = "Invalid referenceType [%s].".formatted(membershipPayload.referenceType());
                log.error(errorDetails, e);
                return Single.just(new DeleteMembershipReply(command.getId(), errorDetails));
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

            membershipService.deleteReferenceMember(
                executionContext,
                membershipReferenceType,
                membershipPayload.referenceId(),
                MembershipMemberType.USER,
                userEntity.getId()
            );

            log.info(
                "Delete reference member with organization [{}] referenceType [{}] referenceId [{}] and memberId [{}]",
                membershipPayload.organizationId(),
                membershipPayload.referenceType(),
                membershipPayload.referenceId(),
                membershipPayload.userId()
            );
            return Single.just(new DeleteMembershipReply(command.getId()));
        } catch (Exception e) {
            String errorDetails =
                "Error occurred when trying to delete member on %s [%s] for cockpit user [%s] and organization [%s].".formatted(
                        membershipPayload.referenceType(),
                        membershipPayload.referenceId(),
                        membershipPayload.userId(),
                        membershipPayload.organizationId()
                    );
            log.error(errorDetails, e);
            return Single.just(new DeleteMembershipReply(command.getId(), errorDetails));
        }
    }
}
