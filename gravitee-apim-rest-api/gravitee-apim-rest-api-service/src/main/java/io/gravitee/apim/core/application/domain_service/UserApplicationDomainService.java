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
package io.gravitee.apim.core.application.domain_service;

import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class UserApplicationDomainService {

    private final MembershipQueryService membershipQueryService;

    /**
     * Returns the set of application IDs for which the given user has a direct membership.
     *
     * @param userId the user ID
     * @return a set of application IDs
     */
    public Set<String> findApplicationIdsByUserId(String userId) {
        return membershipQueryService
            .findByMemberIdAndMemberTypeAndReferenceType(userId, Membership.Type.USER, Membership.ReferenceType.APPLICATION)
            .stream()
            .map(Membership::getReferenceId)
            .collect(toSet());
    }
}
