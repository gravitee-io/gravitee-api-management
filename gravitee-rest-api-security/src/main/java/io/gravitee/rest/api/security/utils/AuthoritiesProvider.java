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
package io.gravitee.rest.api.security.utils;

import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.service.MembershipService;
import org.springframework.security.core.GrantedAuthority;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.security.core.authority.AuthorityUtils.commaSeparatedStringToAuthorityList;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AuthoritiesProvider {

    private final MembershipService membershipService;

    public AuthoritiesProvider(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    public Set<GrantedAuthority> retrieveAuthorities(String userId) {
        final Set<GrantedAuthority> authorities = new HashSet<>();

        final Set<RoleEntity> roles =
                membershipService.getRoles(MembershipReferenceType.PLATFORM, "DEFAULT", MembershipMemberType.USER, userId);
        roles.addAll(membershipService.getRoles(MembershipReferenceType.ORGANIZATION, "DEFAULT", MembershipMemberType.USER, userId));
        roles.addAll(membershipService.getRoles(MembershipReferenceType.ENVIRONMENT, "DEFAULT", MembershipMemberType.USER, userId));

        if (!roles.isEmpty()) {
            authorities.addAll(commaSeparatedStringToAuthorityList(roles.stream()
                    .map(r -> r.getScope().name() + ':' + r.getName()).collect(Collectors.joining(","))));
        }

        return authorities;
    }
}
