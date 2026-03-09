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
package io.gravitee.rest.api.management.v2.rest.utils;

import io.gravitee.rest.api.management.v2.rest.mapper.ApplicationMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.PlanMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.UserMapper;
import io.gravitee.rest.api.management.v2.rest.model.BaseApplication;
import io.gravitee.rest.api.management.v2.rest.model.BasePlan;
import io.gravitee.rest.api.management.v2.rest.model.BaseUser;
import io.gravitee.rest.api.management.v2.rest.model.Subscription;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SubscriptionExpandHelper {

    public static final String EXPAND_PLAN = "plan";
    public static final String EXPAND_APPLICATION = "application";
    public static final String EXPAND_SUBSCRIBED_BY = "subscribedBy";

    private static final PlanMapper planMapper = PlanMapper.INSTANCE;
    private static final ApplicationMapper applicationMapper = ApplicationMapper.INSTANCE;
    private static final UserMapper userMapper = UserMapper.INSTANCE;

    private final PlanSearchService planSearchService;
    private final ApplicationService applicationService;
    private final UserService userService;

    public SubscriptionExpandHelper(PlanSearchService planSearchService, ApplicationService applicationService, UserService userService) {
        this.planSearchService = planSearchService;
        this.applicationService = applicationService;
        this.userService = userService;
    }

    public void expandForApiProduct(
        ExecutionContext executionContext,
        String apiProductId,
        List<Subscription> subscriptions,
        Set<String> expands
    ) {
        if (expands == null || expands.isEmpty()) {
            return;
        }
        if (expands.contains(EXPAND_PLAN)) {
            final Set<String> planIds = subscriptions
                .stream()
                .map(sub -> sub.getPlan().getId())
                .collect(Collectors.toSet());
            final Collection<BasePlan> plans = planMapper.mapToBasePlans(planSearchService.findByIdIn(executionContext, planIds));
            plans.forEach(plan -> {
                plan.setApiId(null);
                plan.setApiProductId(apiProductId);
                subscriptions
                    .stream()
                    .filter(sub -> sub.getPlan().getId().equals(plan.getId()))
                    .forEach(sub -> sub.setPlan(plan));
            });
        }
        if (expands.contains(EXPAND_APPLICATION)) {
            final Set<String> applicationIds = subscriptions
                .stream()
                .map(sub -> sub.getApplication().getId())
                .collect(Collectors.toSet());
            final Collection<BaseApplication> applications = applicationMapper.mapToBaseApplicationList(
                applicationService.findByIds(executionContext, applicationIds)
            );
            applications.forEach(application ->
                subscriptions
                    .stream()
                    .filter(sub -> sub.getApplication().getId().equals(application.getId()))
                    .forEach(sub -> sub.setApplication(application))
            );
        }
        if (expands.contains(EXPAND_SUBSCRIBED_BY)) {
            final Set<String> userIds = subscriptions
                .stream()
                .map(sub -> sub.getSubscribedBy().getId())
                .collect(Collectors.toSet());
            final Collection<BaseUser> users = userMapper.mapToBaseUserList(userService.findByIds(executionContext, userIds));
            users.forEach(user ->
                subscriptions
                    .stream()
                    .filter(sub -> sub.getSubscribedBy().getId().equals(user.getId()))
                    .forEach(sub -> sub.setSubscribedBy(user))
            );
        }
    }
}
