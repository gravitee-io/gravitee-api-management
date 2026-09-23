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
package io.gravitee.apim.infra.domain_service.group;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.domain_service.CreateGroupDomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.NewGroupEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.GroupNameAlreadyExistsException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Delegates empty-group creation to legacy {@link GroupService#create} so import/promotion keeps
 * unique-name enforcement and {@code GROUP_CREATED} audit (V2 {@code ApiDuplicatorService} behavior).
 */
@Service
@RequiredArgsConstructor
public class CreateGroupDomainServiceLegacyWrapper implements CreateGroupDomainService {

    private final GroupService groupService;
    private final GroupQueryService groupQueryService;

    @Override
    public CreateResult createEmpty(String name, AuditInfo auditInfo) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name must not be null or blank");
        }
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        try {
            var newGroupEntity = new NewGroupEntity();
            newGroupEntity.setName(name);
            var created = toCore(groupService.create(executionContext, newGroupEntity), auditInfo.environmentId());
            return new CreateResult(created, true);
        } catch (GroupNameAlreadyExistsException e) {
            // Concurrent create won the race; reuse the existing env-scoped group.
            var existing = groupQueryService.findByNames(auditInfo.environmentId(), Set.of(name));
            if (existing.isEmpty()) {
                throw e;
            }
            return new CreateResult(existing.getFirst(), false);
        }
    }

    private static Group toCore(GroupEntity entity, String fallbackEnvironmentId) {
        var environmentId = entity.getEnvironmentId() != null ? entity.getEnvironmentId() : fallbackEnvironmentId;
        return Group.builder()
            .id(entity.getId())
            .name(entity.getName())
            .environmentId(environmentId)
            .createdAt(toZonedDateTime(entity.getCreatedAt()))
            .updatedAt(toZonedDateTime(entity.getUpdatedAt()))
            .maxInvitation(entity.getMaxInvitation())
            .lockApiRole(entity.isLockApiRole())
            .lockApplicationRole(entity.isLockApplicationRole())
            .systemInvitation(entity.isSystemInvitation())
            .emailInvitation(entity.isEmailInvitation())
            .disableMembershipNotifications(entity.isDisableMembershipNotifications())
            .apiPrimaryOwner(entity.getApiPrimaryOwner())
            .build();
    }

    private static ZonedDateTime toZonedDateTime(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneOffset.UTC);
    }
}
