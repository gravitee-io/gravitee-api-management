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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

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
        portalRole.setId("PORTAL_ROLE");
        portalRole.setName("PORTAL_ROLE");
        portalRole.setScope(RoleScope.ENVIRONMENT);

        final RoleEntity mgtRole1 = new RoleEntity();
        mgtRole1.setId("MGT_ROLE1");
        mgtRole1.setName("MGT_ROLE1");
        mgtRole1.setScope(RoleScope.ORGANIZATION);

        final RoleEntity mgtRole2 = new RoleEntity();
        mgtRole2.setId("MGT_ROLE2");
        mgtRole2.setName("MGT_ROLE2");
        mgtRole2.setScope(RoleScope.ORGANIZATION);

        when(membershipService.getRoles(MembershipReferenceType.ENVIRONMENT, "DEFAULT", MembershipMemberType.USER, USER_ID))
            .thenReturn(new HashSet<>(asList(portalRole)));
        when(membershipService.getRoles(MembershipReferenceType.ORGANIZATION, "DEFAULT", MembershipMemberType.USER, USER_ID))
            .thenReturn(new HashSet<>(asList(mgtRole1, mgtRole2)));

        final Set<GrantedAuthority> grantedAuthorities = cut.retrieveAuthorities(USER_ID);

        assertEquals(3, grantedAuthorities.size());
        final List<GrantedAuthority> expected = AuthorityUtils.commaSeparatedStringToAuthorityList(
            "ENVIRONMENT:PORTAL_ROLE,ORGANIZATION:MGT_ROLE1,ORGANIZATION:MGT_ROLE2"
        );
        assertTrue(grantedAuthorities.containsAll(expected));
    }
}
