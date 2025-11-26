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
package io.gravitee.apim.infra.domain_service.analytics_engine.permissions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ContextConfiguration(classes = { ResourceContextConfiguration.class })
@ExtendWith(SpringExtension.class)
public abstract class AbstractPermissionsTest {

    @Inject
    PermissionService permissionService;

    @Inject
    ApiAuthorizationService apiAuthorizationService;

    @Inject
    ApiAnalyticsQueryFilterDecoratorImpl apiAnalyticsQueryFilterDecorator;

    static final Authentication authentication = mock(Authentication.class);

    @BeforeEach
    public void setUp() {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    void setAuthenticatedUsername(String username) {
        when(authentication.getName()).thenReturn(username);
    }

    void setAuthorities(GrantedAuthority... authorities) {
        Collection authorityList = new ArrayList<>(Arrays.stream(authorities).toList());
        when(authentication.getAuthorities()).thenReturn(authorityList);
    }
}
