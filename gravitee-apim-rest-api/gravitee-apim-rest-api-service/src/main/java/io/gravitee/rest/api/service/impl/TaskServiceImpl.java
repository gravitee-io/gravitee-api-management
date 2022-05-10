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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
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
    private MembershipService membershipService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private PromotionTasksService promotionTasksService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ApiRepository apiRepository;

    @Override
    public List<TaskEntity> findAll(ExecutionContext executionContext, String userId) {
        if (userId == null) {
            throw new UnauthorizedAccessException();
        }

        try {
            // because Tasks only consists on subscriptions, we can optimize the search by only look for apis where
            // the user has a SUBSCRIPTION_UPDATE permission

            // search for PENDING subscriptions
            Set<String> apiIds = getApisForAPermission(executionContext, userId, SUBSCRIPTION.getName());
            final List<TaskEntity> tasks;
            if (apiIds.isEmpty()) {
                tasks = new ArrayList<>();
            } else {
                SubscriptionQuery query = new SubscriptionQuery();
                query.setStatuses(singleton(PENDING));
                query.setApis(apiIds);
                tasks = subscriptionService.search(executionContext, query).stream().map(this::convert).collect(toList());
            }

            if (isEnvironmentAdmin()) {
                // search for PENDING user registration
                final Page<UserEntity> pendingUsers = userService.search(
                    executionContext,
                    new UserCriteria.Builder().statuses(UserStatus.PENDING).build(),
                    new PageableImpl(1, NUMBER_OF_PENDING_USERS_TO_SEARCH)
                );
                if (pendingUsers.getContent() != null && !pendingUsers.getContent().isEmpty()) {
                    tasks.addAll(pendingUsers.getContent().stream().map(this::convert).collect(toList()));
                }
            }

            // search for IN_REVIEW apis
            apiIds = getApisForAPermission(executionContext, userId, REVIEWS.getName());
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
            apiIds = getApisForAPermission(executionContext, userId, DEFINITION.getName());
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
            tasks.addAll(promotionTasksService.getPromotionTasks(executionContext));

            return tasks;
        } catch (TechnicalException e) {
            LOGGER.error("Error retrieving user tasks {}", e.getMessage());
            throw new TechnicalManagementException("Error retreiving user tasks", e);
        }
    }

    private Set<String> getApisForAPermission(ExecutionContext executionContext, final String userId, final String permission)
        throws TechnicalException {
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
            apiIds.addAll(apiService.searchIds(executionContext, apiQuery));
        }

        return apiIds;
    }

    @Override
    public Metadata getMetadata(ExecutionContext executionContext, List<TaskEntity> tasks) {
        final Metadata metadata = new Metadata();
        tasks.forEach(
            task -> {
                final Object data = task.getData();
                if (data instanceof SubscriptionEntity) {
                    final SubscriptionEntity subscription = (SubscriptionEntity) data;

                    if (!metadata.containsKey(subscription.getApplication())) {
                        try {
                            Optional<Application> application = applicationRepository.findById(subscription.getApplication());
                            if (application.isPresent()) {
                                metadata.put(subscription.getApplication(), "name", application.get().getName());
                            }
                        } catch (TechnicalException e) {
                            LOGGER.error("Error retrieving application task metadata {}", e.getMessage());
                        }
                    }

                    if (!metadata.containsKey(subscription.getPlan())) {
                        try {
                            Optional<Plan> optPlan = planRepository.findById(subscription.getPlan());

                            if (optPlan.isPresent()) {
                                String apiId = optPlan.get().getApi();
                                metadata.put(subscription.getPlan(), "name", optPlan.get().getName());
                                metadata.put(subscription.getPlan(), "api", apiId);

                                Optional<Api> optionalApi = apiRepository.findById(apiId);
                                if (optionalApi.isPresent()) {
                                    metadata.put(apiId, "name", optionalApi.get().getName());
                                }
                            }
                        } catch (TechnicalException e) {
                            LOGGER.error("Error retrieving plan task metadata {}", e.getMessage());
                        }
                    }
                } else if (data instanceof Workflow) {
                    final Workflow workflow = (Workflow) data;
                    if (API.name().equals(workflow.getReferenceType()) && !metadata.containsKey(workflow.getReferenceId())) {
                        try {
                            Optional<Api> optionalApi = apiRepository.findById(workflow.getReferenceId());
                            if (optionalApi.isPresent()) {
                                metadata.put(workflow.getReferenceId(), "name", optionalApi.get().getName());
                            }
                        } catch (TechnicalException e) {
                            LOGGER.error("Error retrieving api task metadata {}", e.getMessage());
                        }
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
