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
package io.gravitee.rest.api.idp.repository.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.idp.repository.RepositoryIdentityProvider;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RepositoryAuthenticationProviderTest {

    private static final String USERNAME = "user@example.com";

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RepositoryAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new RepositoryAuthenticationProvider();
        ReflectionTestUtils.setField(provider, "userService", userService);
        ReflectionTestUtils.setField(provider, "passwordEncoder", passwordEncoder);
    }

    @Test
    void should_reject_a_user_whose_registration_is_awaiting_approval_as_an_authentication_failure() {
        when(
            userService.findBySource(nullable(String.class), eq(RepositoryIdentityProvider.PROVIDER_TYPE), eq(USERNAME), eq(true))
        ).thenReturn(aUser("PENDING"));

        assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken(USERNAME, "password")))
            .isInstanceOf(DisabledException.class)
            .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void should_report_an_unknown_user_as_a_bad_credentials_failure() {
        when(
            userService.findBySource(nullable(String.class), eq(RepositoryIdentityProvider.PROVIDER_TYPE), eq(USERNAME), eq(true))
        ).thenThrow(new UserNotFoundException(USERNAME));

        assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken(USERNAME, "password"))).isInstanceOf(
            BadCredentialsException.class
        );
    }

    @Test
    void should_authenticate_an_active_user() {
        when(
            userService.findBySource(nullable(String.class), eq(RepositoryIdentityProvider.PROVIDER_TYPE), eq(USERNAME), eq(true))
        ).thenReturn(aUser("ACTIVE"));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        var authentication = provider.authenticate(new UsernamePasswordAuthenticationToken(USERNAME, "password"));

        assertThat(authentication.isAuthenticated()).isTrue();
    }

    private static UserEntity aUser(String status) {
        UserEntity user = new UserEntity();
        user.setId("user-1");
        user.setSource(RepositoryIdentityProvider.PROVIDER_TYPE);
        user.setSourceId(USERNAME);
        user.setEmail(USERNAME);
        user.setPassword("$2a$10$encoded");
        user.setStatus(status);
        return user;
    }
}
