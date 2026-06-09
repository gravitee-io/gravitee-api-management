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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.GroupInvitation;
import io.gravitee.apim.core.invitation.model.Invitation;
import io.gravitee.apim.core.invitation.model.InvitationId;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.repository.management.model.InvitationReferenceType;
import java.time.ZonedDateTime;
import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface InvitationAdapter {
    InvitationAdapter INSTANCE = Mappers.getMapper(InvitationAdapter.class);

    default ApplicationInvitation toApplicationInvitation(io.gravitee.repository.management.model.Invitation invitation) {
        if (invitation == null) {
            return null;
        }
        return ApplicationInvitation.of(
            map(invitation.getId()),
            invitation.getReferenceId(),
            invitation.getEmail(),
            invitation.getApplicationRole(),
            map(invitation.getCreatedAt()),
            map(invitation.getUpdatedAt())
        );
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "referenceId", source = "referenceId")
    @Mapping(target = "referenceType", source = "referenceType")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "apiRole", source = "apiRole")
    @Mapping(target = "applicationRole", source = "applicationRole")
    GroupInvitation toGroupInvitation(io.gravitee.repository.management.model.Invitation invitation);

    @Mapping(target = "id", expression = "java(map(invitation.id()))")
    @Mapping(target = "referenceId", source = "applicationId")
    @Mapping(target = "referenceType", constant = "APPLICATION")
    @Mapping(target = "email", expression = "java(invitation.email())")
    @Mapping(target = "applicationRole", source = "roleName")
    @Mapping(target = "apiRole", ignore = true)
    io.gravitee.repository.management.model.Invitation toRepository(ApplicationInvitation invitation);

    default Invitation toEntity(io.gravitee.repository.management.model.Invitation invitation) {
        return switch (InvitationReferenceType.valueOf(invitation.getReferenceType())) {
            case GROUP -> toGroupInvitation(invitation);
            case APPLICATION -> toApplicationInvitation(invitation);
            case API -> throw new TechnicalDomainException("Unsupported invitation reference type: API");
        };
    }

    default InvitationId map(String id) {
        return InvitationId.of(id);
    }

    default String map(InvitationId id) {
        return id.toString();
    }

    default ZonedDateTime map(Date date) {
        return date == null ? null : date.toInstant().atZone(TimeProvider.clock().getZone());
    }

    default Date map(ZonedDateTime date) {
        return date == null ? null : Date.from(date.toInstant());
    }
}
