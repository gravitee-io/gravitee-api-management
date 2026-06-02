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
package io.gravitee.rest.api.security.utils;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class AuthoritiesProviderTest {

    public static final String ENVIRONMENT_ID = "environment#id";
    public static final String ORGANIZATION_ID = "organization#id";

    @Mock
    private MembershipService membershipService;

    private AuthoritiesProvider cut;

    @BeforeEach
    public void init() {
        GraviteeContext.fromExecutionContext(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID));
        cut = new AuthoritiesProvider(membershipService);
    }

    @AfterEach
    public void clean() {
        GraviteeContext.cleanContext();
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

        when(
            membershipService.getRoles(MembershipReferenceType.ENVIRONMENT, ENVIRONMENT_ID, MembershipMemberType.USER, USER_ID)
        ).thenReturn(new HashSet<>(asList(portalRole)));
        when(
            membershipService.getRoles(MembershipReferenceType.ORGANIZATION, ORGANIZATION_ID, MembershipMemberType.USER, USER_ID)
        ).thenReturn(new HashSet<>(asList(mgtRole1, mgtRole2)));

        final Set<GrantedAuthority> grantedAuthorities = cut.retrieveAuthorities(USER_ID);

        assertEquals(3, grantedAuthorities.size());
        final List<GrantedAuthority> expected = AuthorityUtils.commaSeparatedStringToAuthorityList(
            "ENVIRONMENT:PORTAL_ROLE,ORGANIZATION:MGT_ROLE1,ORGANIZATION:MGT_ROLE2"
        );
        assertTrue(grantedAuthorities.containsAll(expected));
    }
}
