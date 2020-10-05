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
package io.gravitee.management.security.utils;

import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.service.MembershipService;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
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
                membershipService.getRoles(MembershipReferenceType.PORTAL, singleton(MembershipDefaultReferenceId.DEFAULT.name()), userId, RoleScope.PORTAL);
        roles.addAll(membershipService.getRoles(MembershipReferenceType.MANAGEMENT, singleton(MembershipDefaultReferenceId.DEFAULT.name()), userId, RoleScope.MANAGEMENT));

        if (!roles.isEmpty()) {
            authorities.addAll(commaSeparatedStringToAuthorityList(roles.stream()
                    .map(r -> r.getScope().name() + ':' + r.getName()).collect(Collectors.joining(","))));
        }

        return authorities;
    }
}
