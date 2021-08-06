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
package io.gravitee.rest.api.idp.ldap.authentication;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapAuthority;

public class UserDetailsContextPropertiesMapperTest {

    private UserDetailsContextPropertiesMapper cut;
    private Environment environment;
    private DirContextOperations ctx;

    @Before
    public void setUp() throws Exception {
        environment = mock(Environment.class);
        ctx = mock(DirContextOperations.class);
        cut = new UserDetailsContextPropertiesMapper();
        cut.setEnvironment(environment);
        cut.afterPropertiesSet();

        when(ctx.getStringAttribute("uid")).thenReturn("uid");
    }

    @Test
    public void shouldAddMappedAuthorities() {
        when(environment.getProperty("authentication.group.role.mapper.multiple"))
            .thenReturn("ORGANIZATION:USER,ENVIRONMENT:API_PUBLISHER");

        LdapAuthority authority = new LdapAuthority("multiple", "dn");

        final UserDetails result = cut.mapUserFromContext(ctx, "username", Collections.singleton(authority));

        assertEquals(result.getAuthorities().size(), 2);
    }

    @Test
    public void shouldAddSimpleMappedAuthorities() {
        when(environment.getProperty("authentication.group.role.mapper.simple")).thenReturn("API_CONSUMER");

        LdapAuthority authority = new LdapAuthority("simple", "dn");

        final UserDetails result = cut.mapUserFromContext(ctx, "username", Collections.singleton(authority));

        assertEquals(result.getAuthorities().size(), 1);
    }

    @Test
    public void shouldAddMixedMappedAuthorities() {
        when(environment.getProperty("authentication.group.role.mapper.simple")).thenReturn("API_CONSUMER");
        when(environment.getProperty("authentication.group.role.mapper.multiple"))
            .thenReturn("ORGANIZATION:USER,ENVIRONMENT:API_PUBLISHER");

        LdapAuthority authority = new LdapAuthority("simple", "dn");
        LdapAuthority authority2 = new LdapAuthority("multiple", "dn");

        final UserDetails result = cut.mapUserFromContext(ctx, "username", Arrays.asList(authority, authority2));

        assertEquals(result.getAuthorities().size(), 3);
    }
}
