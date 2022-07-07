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
package io.gravitee.rest.api.service.v4.impl.validation;

import static java.util.stream.Collectors.toSet;

import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
<<<<<<< HEAD
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
=======
>>>>>>> cea4485fb9 (feat(definition): add api definition v4 classes)
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.GroupsNotFoundException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.validation.GroupValidationService;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class GroupValidationServiceImpl extends TransactionalService implements GroupValidationService {

    private final GroupService groupService;
    private final MembershipService membershipService;

    public GroupValidationServiceImpl(final GroupService groupService, final MembershipService membershipService) {
        this.groupService = groupService;
        this.membershipService = membershipService;
    }

    @Override
<<<<<<< HEAD
    public Set<String> validateAndSanitize(
        final ExecutionContext executionContext,
        final Set<String> groups,
        final PrimaryOwnerEntity primaryOwnerEntity
    ) {
=======
    public Set<String> validateAndSanitize(final ExecutionContext executionContext, final Set<String> groups) {
>>>>>>> cea4485fb9 (feat(definition): add api definition v4 classes)
        Set<String> sanitizedGroups = new HashSet<>();
        if (groups != null && !groups.isEmpty()) {
            checkGroupExistence(groups);
            sanitizedGroups = removePrimaryOwnerGroups(executionContext, groups, null);
        }

        // Add default group
        Set<String> defaultGroups = groupService
            .findByEvent(executionContext.getEnvironmentId(), GroupEvent.API_CREATE)
            .stream()
            .map(GroupEntity::getId)
            .collect(toSet());
        sanitizedGroups.addAll(defaultGroups);

        // if primary owner is a group, add it as a member of the API
<<<<<<< HEAD
        if (ApiPrimaryOwnerMode.GROUP.name().equals(primaryOwnerEntity.getType())) {
            sanitizedGroups.add(primaryOwnerEntity.getId());
=======
        if (ApiPrimaryOwnerMode.GROUP.name().equals(primaryOwner.getType())) {
            if (repoApi.getGroups() == null) {
                repoApi.setGroups(new HashSet<>());
            }
            repoApi.getGroups().add(primaryOwner.getId());
>>>>>>> cea4485fb9 (feat(definition): add api definition v4 classes)
        }

        return sanitizedGroups;
    }

    private void checkGroupExistence(final Set<String> groups) {
        // check the existence of groups
        if (groups != null && !groups.isEmpty()) {
            try {
                groupService.findByIds(groups);
            } catch (GroupsNotFoundException gnfe) {
                throw new InvalidDataException("These groups [" + gnfe.getParameters().get("groups") + "] do not exist");
            }
        }
    }

    private Set<String> removePrimaryOwnerGroups(final ExecutionContext executionContext, final Set<String> groups, String apiId) {
        Stream<GroupEntity> groupEntityStream = groupService.findByIds(groups).stream();
        if (apiId != null) {
            final MembershipEntity primaryOwner = membershipService.getPrimaryOwner(
                executionContext.getOrganizationId(),
                MembershipReferenceType.API,
                apiId
            );
            if (primaryOwner.getMemberType() == MembershipMemberType.GROUP) {
                // don't remove the primary owner group of this API.
                groupEntityStream =
<<<<<<< HEAD
                    groupEntityStream.filter(
                        group -> StringUtils.isEmpty(group.getApiPrimaryOwner()) || group.getId().equals(primaryOwner.getMemberId())
=======
                    groupEntityStream.filter(group ->
                        StringUtils.isEmpty(group.getApiPrimaryOwner()) || group.getId().equals(primaryOwner.getMemberId())
>>>>>>> 2a8318ccf0 (feat(definition): add api definition v4 classes)
                    );
            } else {
                groupEntityStream = groupEntityStream.filter(group -> StringUtils.isEmpty(group.getApiPrimaryOwner()));
            }
        } else {
            groupEntityStream = groupEntityStream.filter(group -> StringUtils.isEmpty(group.getApiPrimaryOwner()));
        }

        return groupEntityStream.map(GroupEntity::getId).collect(Collectors.toSet());
    }
}
