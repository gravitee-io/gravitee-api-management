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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.model.Plan;
import io.gravitee.rest.api.portal.rest.model.Plan.SecurityEnum;
import io.gravitee.rest.api.portal.rest.model.Plan.ValidationEnum;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PlanMapper {

    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    ApplicationService applicationService;

    public Plan convert(PlanEntity plan, String user) {
        final Plan planItem = new Plan();

        planItem.setCharacteristics(plan.getCharacteristics());
        planItem.setCommentQuestion(plan.getCommentMessage());
        planItem.setCommentRequired(Boolean.valueOf(plan.isCommentRequired()));
        planItem.setDescription(plan.getDescription());
        planItem.setId(plan.getId());
        planItem.setName(plan.getName());
        planItem.setOrder(Integer.valueOf(plan.getOrder()));
        planItem.setSecurity(SecurityEnum.fromValue(plan.getSecurity().name()));
        planItem.setValidation(ValidationEnum.fromValue(plan.getValidation().name()));
        if (user != null) {
            Set<ApplicationListItem> usersApplications = applicationService.findByUser(user);

            SubscriptionQuery query = new SubscriptionQuery();
            query.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED));
            query.setPlan(plan.getId());
            query.setApplications(usersApplications.stream().map(ApplicationListItem::getId).collect(Collectors.toList()));

            Collection<SubscriptionEntity> subscriptions = subscriptionService.search(query);

            planItem.setSubscribed(subscriptions != null && !subscriptions.isEmpty());
        }
        return planItem;
    }

}
