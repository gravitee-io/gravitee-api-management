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
package io.gravitee.management.service.impl;

import io.gravitee.management.model.*;
import io.gravitee.management.model.pagedresult.Metadata;
import io.gravitee.management.model.subscription.SubscriptionQuery;
import io.gravitee.management.service.*;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.exceptions.UnauthorizedAccessException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.management.model.SubscriptionStatus.PENDING;
import static io.gravitee.management.model.permissions.ApiPermission.SUBSCRIPTION;

/**
 * @author Nicolas GERAUD(nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class TaskServiceImpl extends AbstractService implements TaskService {

    private final Logger LOGGER = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    ApiService apiService;

    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    ApplicationService applicationService;

    @Autowired
    MembershipRepository membershipRepository;

    @Autowired
    ApiRepository apiRepository;

    @Autowired
    RoleService roleService;

    @Autowired
    PlanService planService;

    @Override
    public List<TaskEntity> findAll(String userId) {
        if (userId == null) {
            throw new UnauthorizedAccessException();
        }

        try {
            // because Tasks only consists on subscriptions, we can optimize the search by only look for apis where
            // the user has a SUBSCRIPTION_UPDATE permission

            // 1. find apis and group memberships
            Set<Membership> memberships = membershipRepository.findByUserAndReferenceType(userId, MembershipReferenceType.GROUP);
            memberships.addAll(membershipRepository.findByUserAndReferenceType(userId, MembershipReferenceType.API));

            Map<String, RoleEntity> roleNameToEntity = new HashMap<>();
            Set<String> apiIds = new HashSet<>();
            List<String> groupIds = new ArrayList<>();
            for (Membership membership : memberships) {
                // 2. get API roles in each memberships (in GROUP, it could be null)
                String roleName = membership.getRoles().get(RoleScope.API.getId());
                if (roleName != null) {
                    // 3. search for roleEntity only once
                    RoleEntity roleEntity = roleNameToEntity.get(roleName);
                    if (roleEntity == null) {
                        roleEntity = roleService.findById(RoleScope.API, roleName);
                        roleNameToEntity.put(roleName, roleEntity);
                    }
                    // 4. get apiId or groupIId only if the role has a SUBSCRIPTIONS_UPDATE permission
                    for (char c : roleEntity.getPermissions().get(SUBSCRIPTION.getName())) {
                        if (c == 'U') {
                            switch(membership.getReferenceType()) {
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

            // 5. add apiId that comes from group
            apiIds.addAll(apiRepository
                    .findByGroups(groupIds)
                    .stream()
                    .map(Api::getId)
                    .collect(Collectors.toSet()));

            // 6. search for PENDING subscriptions
            SubscriptionQuery query = new SubscriptionQuery();
            query.setStatuses(Collections.singleton(PENDING));
            query.setApis(apiIds);
            return subscriptionService.search(query)
                    .stream()
                    .map(this::convert)
                    .collect(Collectors.toList());
        } catch (TechnicalException e) {
            LOGGER.error("Error retreiving user tasks {}", e.getMessage());
            throw new TechnicalManagementException("Error retreiving user tasks", e);
        }
    }

    public Metadata getMetadata(List<TaskEntity> tasks) {
        Metadata metadata = new Metadata();
        tasks.forEach( task -> {
            SubscriptionEntity subscription = (SubscriptionEntity)task.getData();

            if (!metadata.containsKey(subscription.getApplication())) {
                ApplicationEntity applicationEntity = applicationService.findById(subscription.getApplication());
                metadata.put(subscription.getApplication(), "name", applicationEntity.getName());
            }

            if (!metadata.containsKey(subscription.getPlan())) {
                PlanEntity planEntity = planService.findById(subscription.getPlan());
                String apiId = planEntity.getApis().iterator().next();
                ApiEntity api = apiService.findById(apiId);
                metadata.put(subscription.getPlan(), "name", planEntity.getName());
                metadata.put(subscription.getPlan(), "api", apiId);
                metadata.put(apiId, "name", api.getName());
            }
        });
        return metadata;
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
}
