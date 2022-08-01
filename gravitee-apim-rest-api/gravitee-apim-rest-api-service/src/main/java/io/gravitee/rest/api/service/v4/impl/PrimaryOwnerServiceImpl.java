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
package io.gravitee.rest.api.service.v4.impl;

import static java.util.stream.Collectors.toSet;

import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import io.gravitee.rest.api.service.exceptions.NoPrimaryOwnerGroupForUserException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("PrimaryOwnerServiceImplV4")
@Slf4j
public class PrimaryOwnerServiceImpl extends TransactionalService implements PrimaryOwnerService {

    private final UserService userService;
    private final MembershipService membershipService;
    private final GroupService groupService;
    private final ParameterService parameterService;

    public PrimaryOwnerServiceImpl(
        final UserService userService,
        final MembershipService membershipService,
        final GroupService groupService,
        final ParameterService parameterService
    ) {
        this.userService = userService;
        this.membershipService = membershipService;
        this.groupService = groupService;
        this.parameterService = parameterService;
    }

    @Override
    public PrimaryOwnerEntity getPrimaryOwner(final ExecutionContext executionContext, final String apiId)
        throws TechnicalManagementException {
        MembershipEntity primaryOwnerMemberEntity = membershipService.getPrimaryOwner(
            executionContext.getOrganizationId(),
            io.gravitee.rest.api.model.MembershipReferenceType.API,
            apiId
        );
        if (primaryOwnerMemberEntity == null) {
            log.error("The API {} doesn't have any primary owner.", apiId);
            throw new TechnicalManagementException("The API " + apiId + " doesn't have any primary owner.");
        }
        if (MembershipMemberType.GROUP == primaryOwnerMemberEntity.getMemberType()) {
            return new PrimaryOwnerEntity(groupService.findById(executionContext, primaryOwnerMemberEntity.getMemberId()));
        }
        return new PrimaryOwnerEntity(userService.findById(executionContext, primaryOwnerMemberEntity.getMemberId()));
    }

    @Override
    public PrimaryOwnerEntity getPrimaryOwner(
        final ExecutionContext executionContext,
        final String userId,
        final PrimaryOwnerEntity currentPrimaryOwner
    ) {
        ApiPrimaryOwnerMode poMode = ApiPrimaryOwnerMode.valueOf(
            this.parameterService.find(executionContext, Key.API_PRIMARY_OWNER_MODE, ParameterReferenceType.ENVIRONMENT)
        );
        switch (poMode) {
            case USER:
                if (currentPrimaryOwner == null || ApiPrimaryOwnerMode.GROUP.name().equals(currentPrimaryOwner.getType())) {
                    return new PrimaryOwnerEntity(userService.findById(executionContext, userId));
                }
                if (ApiPrimaryOwnerMode.USER.name().equals(currentPrimaryOwner.getType())) {
                    try {
                        return new PrimaryOwnerEntity(userService.findById(executionContext, currentPrimaryOwner.getId()));
                    } catch (UserNotFoundException unfe) {
                        return new PrimaryOwnerEntity(userService.findById(executionContext, userId));
                    }
                }
                break;
            case GROUP:
                if (currentPrimaryOwner == null) {
                    return getFirstPoGroupUserBelongsTo(userId);
                }
                if (ApiPrimaryOwnerMode.GROUP.name().equals(currentPrimaryOwner.getType())) {
                    try {
                        return new PrimaryOwnerEntity(groupService.findById(executionContext, currentPrimaryOwner.getId()));
                    } catch (GroupNotFoundException unfe) {
                        return getFirstPoGroupUserBelongsTo(userId);
                    }
                }
                if (ApiPrimaryOwnerMode.USER.name().equals(currentPrimaryOwner.getType())) {
                    try {
                        final String poUserId = currentPrimaryOwner.getId();
                        userService.findById(executionContext, poUserId);
                        final Set<GroupEntity> poGroupsOfPoUser = groupService
                            .findByUser(poUserId)
                            .stream()
                            .filter(group -> group.getApiPrimaryOwner() != null && !group.getApiPrimaryOwner().isEmpty())
                            .collect(toSet());
                        if (poGroupsOfPoUser.isEmpty()) {
                            return getFirstPoGroupUserBelongsTo(userId);
                        }
                        return new PrimaryOwnerEntity(poGroupsOfPoUser.iterator().next());
                    } catch (UserNotFoundException unfe) {
                        return getFirstPoGroupUserBelongsTo(userId);
                    }
                }
                break;
            case HYBRID:
            default:
                if (currentPrimaryOwner == null) {
                    return new PrimaryOwnerEntity(userService.findById(executionContext, userId));
                }
                if (ApiPrimaryOwnerMode.GROUP.name().equals(currentPrimaryOwner.getType())) {
                    try {
                        return new PrimaryOwnerEntity(groupService.findById(executionContext, currentPrimaryOwner.getId()));
                    } catch (GroupNotFoundException unfe) {
                        try {
                            return getFirstPoGroupUserBelongsTo(userId);
                        } catch (NoPrimaryOwnerGroupForUserException ex) {
                            return new PrimaryOwnerEntity(userService.findById(executionContext, userId));
                        }
                    }
                }
                if (ApiPrimaryOwnerMode.USER.name().equals(currentPrimaryOwner.getType())) {
                    try {
                        return new PrimaryOwnerEntity(userService.findById(executionContext, currentPrimaryOwner.getId()));
                    } catch (UserNotFoundException unfe) {
                        return new PrimaryOwnerEntity(userService.findById(executionContext, userId));
                    }
                }
                break;
        }

        return new PrimaryOwnerEntity(userService.findById(executionContext, userId));
    }

    @NotNull
    private PrimaryOwnerEntity getFirstPoGroupUserBelongsTo(final String userId) {
        final Set<GroupEntity> poGroupsOfCurrentUser = groupService
            .findByUser(userId)
            .stream()
            .filter(group -> !StringUtils.isEmpty(group.getApiPrimaryOwner()))
            .collect(toSet());
        if (poGroupsOfCurrentUser.isEmpty()) {
            throw new NoPrimaryOwnerGroupForUserException(userId);
        }
        return new PrimaryOwnerEntity(poGroupsOfCurrentUser.iterator().next());
    }
}
