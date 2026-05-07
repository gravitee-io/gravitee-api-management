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
package io.gravitee.apim.core.api_product.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Resolves the API Products a user can access via direct memberships and via group memberships.
 * <p>
 * Group-based API Product access is stored on the API Product itself (the {@code groups} field) when
 * a group is assigned, not as a {@code GROUP -> API_PRODUCT} membership row. To match the behaviour
 * of the API authorization path, both sources must be unioned.
 */
@DomainService
@RequiredArgsConstructor
public class ApiProductAccessibleIdsDomainService {

    private final ApiProductQueryService apiProductQueryService;
    private final MembershipQueryService membershipQueryService;

    /**
     * Returns the union of direct API Product memberships and API Products inherited via the user's
     * group memberships, scoped to the given environment.
     *
     * @param environmentId the environment to scope group-derived lookups to
     * @param userId the user whose memberships are evaluated
     * @return the set of API Product ids the user can see
     */
    public Set<String> findAccessibleApiProductIds(String environmentId, String userId) {
        Set<String> directIds = membershipQueryService
            .findByMemberIdAndMemberTypeAndReferenceType(userId, Membership.Type.USER, Membership.ReferenceType.API_PRODUCT)
            .stream()
            .map(Membership::getReferenceId)
            .collect(Collectors.toSet());

        Set<String> userGroupIds = membershipQueryService
            .findGroupsThatUserBelongsTo(userId)
            .stream()
            .map(Membership::getReferenceId)
            .collect(Collectors.toSet());

        Set<String> groupApiProductIds = userGroupIds.isEmpty()
            ? Set.of()
            : apiProductQueryService.findIdsByEnvironmentIdAndGroups(environmentId, userGroupIds);

        Set<String> allowedIds = new HashSet<>(directIds);
        allowedIds.addAll(groupApiProductIds);
        return allowedIds;
    }
}
