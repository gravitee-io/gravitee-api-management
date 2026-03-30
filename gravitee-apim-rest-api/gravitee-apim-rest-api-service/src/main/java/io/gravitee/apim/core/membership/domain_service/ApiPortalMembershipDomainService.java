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
package io.gravitee.apim.core.membership.domain_service;

import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.application.domain_service.UserApplicationDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ApiPortalMembershipDomainService {

    private final MembershipQueryService membershipQueryService;
    private final SubscriptionQueryService subscriptionQueryService;
    private final UserApplicationDomainService userApplicationDomainService;

    public Set<String> filterApiIdsByUserMembership(String userId, Set<String> candidateApiIds) {
        if (candidateApiIds.isEmpty()) {
            return Set.of();
        }

        Set<String> directApiIds = membershipQueryService
            .findByMemberIdAndMemberTypeAndReferenceType(userId, Membership.Type.USER, Membership.ReferenceType.API)
            .stream()
            .map(Membership::getReferenceId)
            .collect(toSet());

        Set<String> userGroupIds = membershipQueryService
            .findGroupsThatUserBelongsTo(userId)
            .stream()
            .map(Membership::getReferenceId)
            .collect(toSet());

        Set<String> groupApiIds = userGroupIds.isEmpty()
            ? Set.of()
            : membershipQueryService
                .findByMemberIdsAndMemberTypeAndReferenceType(userGroupIds, Membership.Type.GROUP, Membership.ReferenceType.API)
                .stream()
                .map(Membership::getReferenceId)
                .collect(toSet());

        Set<String> allowed = new HashSet<>(directApiIds);
        allowed.addAll(groupApiIds);
        allowed.retainAll(candidateApiIds);
        return allowed;
    }

    public Set<String> filterAllowedApiIdsBySubscription(String userId, Set<String> candidateApiIds) {
        if (candidateApiIds.isEmpty()) {
            return Set.of();
        }

        Set<String> userApplicationIds = userApplicationDomainService.findApplicationIdsByUserId(userId);

        if (userApplicationIds.isEmpty()) {
            return Set.of();
        }

        return subscriptionQueryService
            .findActiveByApplicationIdsAndApiIds(userApplicationIds, candidateApiIds)
            .stream()
            .map(SubscriptionEntity::getApiId)
            .filter(candidateApiIds::contains)
            .collect(toSet());
    }
}
