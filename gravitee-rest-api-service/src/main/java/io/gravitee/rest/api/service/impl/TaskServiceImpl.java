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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.SubscriptionStatus.PENDING;
import static io.gravitee.rest.api.model.WorkflowReferenceType.API;
import static io.gravitee.rest.api.model.permissions.ApiPermission.*;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.UserStatus;
import io.gravitee.repository.management.model.Workflow;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import io.gravitee.rest.api.service.promotion.PromotionTasksService;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD(nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class TaskServiceImpl extends AbstractService implements TaskService {

    private final Logger LOGGER = LoggerFactory.getLogger(TaskServiceImpl.class);
    private static final int NUMBER_OF_PENDING_USERS_TO_SEARCH = 100;

    @Autowired
    private ApiService apiService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PlanService planService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private PromotionTasksService promotionTasksService;

    @Override
    public List<TaskEntity> findAll(String userId) {
        if (userId == null) {
            throw new UnauthorizedAccessException();
        }

        try {
            // because Tasks only consists on subscriptions, we can optimize the search by only look for apis where
            // the user has a SUBSCRIPTION_UPDATE permission

            // search for PENDING subscriptions
            Set<String> apiIds = getApisForAPermission(userId, SUBSCRIPTION.getName());
            final List<TaskEntity> tasks;
            if (apiIds.isEmpty()) {
                tasks = new ArrayList<>();
            } else {
                SubscriptionQuery query = new SubscriptionQuery();
                query.setStatuses(singleton(PENDING));
                query.setApis(apiIds);
                tasks = subscriptionService.search(query).stream().map(this::convert).collect(toList());
            }

            // search for PENDING user registration
            final Page<UserEntity> pendingUsers = userService.search(
                new UserCriteria.Builder().statuses(UserStatus.PENDING).build(),
                new PageableImpl(1, NUMBER_OF_PENDING_USERS_TO_SEARCH)
            );
            if (pendingUsers.getContent() != null && !pendingUsers.getContent().isEmpty()) {
                tasks.addAll(pendingUsers.getContent().stream().map(this::convert).collect(toList()));
            }

            // search for IN_REVIEW apis
            apiIds = getApisForAPermission(userId, REVIEWS.getName());
            if (!apiIds.isEmpty()) {
                apiIds.forEach(
                    apiId -> {
                        final List<Workflow> workflows = workflowService.findByReferenceAndType(API, apiId, WorkflowType.REVIEW);
                        if (workflows != null && !workflows.isEmpty()) {
                            final Workflow currentWorkflow = workflows.get(0);
                            if (WorkflowState.IN_REVIEW.name().equals(currentWorkflow.getState())) {
                                tasks.add(convert(currentWorkflow));
                            }
                        }
                    }
                );
            }

            // search for REQUEST_FOR_CHANGES apis
            apiIds = getApisForAPermission(userId, DEFINITION.getName());
            if (!apiIds.isEmpty()) {
                apiIds.forEach(
                    apiId -> {
                        final List<Workflow> workflows = workflowService.findByReferenceAndType(API, apiId, WorkflowType.REVIEW);
                        if (workflows != null && !workflows.isEmpty()) {
                            final Workflow currentWorkflow = workflows.get(0);
                            if (WorkflowState.REQUEST_FOR_CHANGES.name().equals(currentWorkflow.getState())) {
                                tasks.add(convert(currentWorkflow));
                            }
                        }
                    }
                );
            }

            // search for TO_BE_VALIDATED promotions
            tasks.addAll(promotionTasksService.getPromotionTasks(GraviteeContext.getCurrentOrganization()));

            return tasks;
        } catch (TechnicalException e) {
            LOGGER.error("Error retrieving user tasks {}", e.getMessage());
            throw new TechnicalManagementException("Error retreiving user tasks", e);
        }
    }

    private Set<String> getApisForAPermission(final String userId, final String permission) throws TechnicalException {
        // 1. find apis and group memberships
        Set<MembershipEntity> memberships = membershipService.getMembershipsByMemberAndReference(
            MembershipMemberType.USER,
            userId,
            io.gravitee.rest.api.model.MembershipReferenceType.API
        );
        memberships.addAll(
            membershipService.getMembershipsByMemberAndReference(
                MembershipMemberType.USER,
                userId,
                io.gravitee.rest.api.model.MembershipReferenceType.GROUP
            )
        );

        Map<String, RoleEntity> roleNameToEntity = new HashMap<>();
        Set<String> apiIds = new HashSet<>();
        List<String> groupIds = new ArrayList<>();

        for (MembershipEntity membership : memberships) {
            // 2. get API roles in each memberships and search for roleEntity only once
            RoleEntity roleEntity = roleNameToEntity.get(membership.getRoleId());
            if (roleEntity == null && !roleNameToEntity.containsKey(membership.getRoleId())) {
                RoleEntity role = roleService.findById(membership.getRoleId());
                if (role.getScope() == RoleScope.API) {
                    roleNameToEntity.put(role.getId(), role);
                    roleEntity = role;
                }
            }
            if (roleEntity != null) {
                // 3. get apiId or groupId only if the role has a given permission
                final char[] rights = roleEntity.getPermissions().get(permission);
                if (rights != null) {
                    for (char c : rights) {
                        if (c == 'U') {
                            switch (membership.getReferenceType()) {
                                case GROUP:
                                    groupIds.add(membership.getReferenceId());
                                    break;
                                case API:
                                    apiIds.add(membership.getReferenceId());
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }
        // 54. add apiId that comes from group
        if (!groupIds.isEmpty()) {
            ApiQuery apiQuery = new ApiQuery();
            apiQuery.setGroups(groupIds);
            apiIds.addAll(apiService.searchIds(apiQuery));
        }

        return apiIds;
    }

    @Override
    public Metadata getMetadata(List<TaskEntity> tasks) {
        final Metadata metadata = new Metadata();
        tasks.forEach(
            task -> {
                final Object data = task.getData();
                if (data instanceof SubscriptionEntity) {
                    final SubscriptionEntity subscription = (SubscriptionEntity) data;

                    if (!metadata.containsKey(subscription.getApplication())) {
                        ApplicationEntity applicationEntity = applicationService.findById(subscription.getApplication());
                        metadata.put(subscription.getApplication(), "name", applicationEntity.getName());
                    }

                    if (!metadata.containsKey(subscription.getPlan())) {
                        PlanEntity planEntity = planService.findById(subscription.getPlan());
                        String apiId = planEntity.getApi();
                        ApiEntity api = apiService.findById(apiId);
                        metadata.put(subscription.getPlan(), "name", planEntity.getName());
                        metadata.put(subscription.getPlan(), "api", apiId);
                        metadata.put(apiId, "name", api.getName());
                    }
                } else if (data instanceof Workflow) {
                    final Workflow workflow = (Workflow) data;
                    if (API.name().equals(workflow.getReferenceType()) && !metadata.containsKey(workflow.getReferenceId())) {
                        ApiEntity api = apiService.findById(workflow.getReferenceId());
                        metadata.put(api.getId(), "name", api.getName());
                    }
                }
            }
        );
        return metadata;
    }

    private TaskEntity convert(UserEntity user) {
        TaskEntity taskEntity = new TaskEntity();
        try {
            taskEntity.setType(TaskType.USER_REGISTRATION_APPROVAL);
            taskEntity.setCreatedAt(user.getCreatedAt());
            taskEntity.setData(user);
        } catch (Exception e) {
            LOGGER.error("Error converting user {} to a Task", user.getId());
            throw new TechnicalManagementException("Error converting user " + user.getId() + " to a Task", e);
        }
        return taskEntity;
    }

    private TaskEntity convert(SubscriptionEntity subscription) {
        TaskEntity taskEntity = new TaskEntity();
        try {
            taskEntity.setType(TaskType.SUBSCRIPTION_APPROVAL);
            taskEntity.setCreatedAt(subscription.getCreatedAt());
            taskEntity.setData(subscription);
        } catch (Exception e) {
            LOGGER.error("Error converting subscription {} to a Task", subscription.getId());
            throw new TechnicalManagementException("Error converting subscription " + subscription.getId() + " to a Task", e);
        }
        return taskEntity;
    }

    private TaskEntity convert(Workflow workflow) {
        TaskEntity taskEntity = new TaskEntity();
        try {
            taskEntity.setType(TaskType.valueOf(workflow.getState()));
            taskEntity.setCreatedAt(workflow.getCreatedAt());
            taskEntity.setData(workflow);
        } catch (Exception e) {
            final String error = "Error converting workflow " + workflow.getId() + " to a Task";
            LOGGER.error(error);
            throw new TechnicalManagementException(error, e);
        }
        return taskEntity;
    }
}
