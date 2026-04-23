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
package io.gravitee.apim.infra.query_service.invitation;

import io.gravitee.apim.core.invitation.model.ApplicationInvitationId;
import io.gravitee.apim.core.invitation.model.ApplicationInvitationItem;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.repository.management.model.Invitation;
import java.time.ZonedDateTime;
import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApplicationInvitationItemMapper {
    ApplicationInvitationItemMapper INSTANCE = Mappers.getMapper(ApplicationInvitationItemMapper.class);

    @Mapping(target = "roleName", source = "applicationRole")
    ApplicationInvitationItem toApplicationInvitationItem(Invitation invitation);

    default ApplicationInvitationId map(String id) {
        return ApplicationInvitationId.of(id);
    }

    default ZonedDateTime map(Date date) {
        return date == null ? null : date.toInstant().atZone(TimeProvider.clock().getZone());
    }
}
