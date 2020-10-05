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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthoritiesProviderTest {

    @Mock
    private MembershipService membershipService;

    private AuthoritiesProvider cut;


    @Before
    public void init() {
        cut = new AuthoritiesProvider(membershipService);
    }

    @Test
    public void shouldGenerateAuthorities() {

        final String USER_ID = "userid1";
        final RoleEntity portalRole = new RoleEntity();
        portalRole.setName("PORTAL_ROLE");
        portalRole.setScope(io.gravitee.management.model.permissions.RoleScope.PORTAL);

        final RoleEntity mgtRole1 = new RoleEntity();
        mgtRole1.setName("MGT_ROLE1");
        mgtRole1.setScope(io.gravitee.management.model.permissions.RoleScope.MANAGEMENT);

        final RoleEntity mgtRole2 = new RoleEntity();
        mgtRole2.setName("MGT_ROLE2");
        mgtRole2.setScope(io.gravitee.management.model.permissions.RoleScope.MANAGEMENT);

        when(membershipService.getRoles(MembershipReferenceType.PORTAL, singleton(MembershipDefaultReferenceId.DEFAULT.name()), USER_ID, RoleScope.PORTAL)).thenReturn(new HashSet<>(asList(portalRole)));
        when(membershipService.getRoles(MembershipReferenceType.MANAGEMENT, singleton(MembershipDefaultReferenceId.DEFAULT.name()), USER_ID, RoleScope.MANAGEMENT)).thenReturn(new HashSet<>(asList(mgtRole1, mgtRole2)));

        final Set<GrantedAuthority> grantedAuthorities = cut.retrieveAuthorities(USER_ID);

        assertEquals(3, grantedAuthorities.size());
        final List<GrantedAuthority> expected = AuthorityUtils.commaSeparatedStringToAuthorityList("PORTAL:PORTAL_ROLE,MANAGEMENT:MGT_ROLE1,MANAGEMENT:MGT_ROLE2");
        assertTrue(grantedAuthorities.containsAll(expected));
    }

}