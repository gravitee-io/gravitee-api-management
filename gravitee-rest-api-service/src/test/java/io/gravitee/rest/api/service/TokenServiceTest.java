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
package io.gravitee.rest.api.service;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TokenRepository;
import io.gravitee.repository.management.model.Token;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.NewTokenEntity;
import io.gravitee.rest.api.model.TokenEntity;
import io.gravitee.rest.api.service.exceptions.TokenNameAlreadyExistsException;
import io.gravitee.rest.api.service.impl.TokenServiceImpl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Sets.newHashSet;
import static io.gravitee.repository.management.model.Token.AuditEvent.TOKEN_CREATED;
import static io.gravitee.repository.management.model.Token.AuditEvent.TOKEN_DELETED;
import static io.gravitee.rest.api.model.TokenReferenceType.USER;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class TokenServiceTest {

    private static final String USER_ID = "user123";
    private static final String TOKEN_ID = "1";

    @InjectMocks
    private final TokenService tokenService = new TokenServiceImpl();

    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private Token token;
    @Mock
    private PasswordEncoder passwordEncoder;

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(new SecurityContext() {
            @Override
            public Authentication getAuthentication() {
                return null;
            }
            @Override
            public void setAuthentication(Authentication authentication) {
            }
        });
    }

    @Before
    public void init() throws TechnicalException {
        setField(tokenService, "passwordEncoder", passwordEncoder);
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        when(token.getId()).thenReturn(TOKEN_ID);
        when(token.getName()).thenReturn("name");
        when(token.getToken()).thenReturn("token");
        when(token.getCreatedAt()).thenReturn(new Date(1486771200000L));
        when(token.getExpiresAt()).thenReturn(new Date(1486772200000L));
        when(token.getLastUseAt()).thenReturn(new Date(1486773200000L));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(of(token));

        SecurityContextHolder.setContext(new SecurityContext() {
            @Override
            public Authentication getAuthentication() {
                return new Authentication() {
                    @Override
                    public Collection<? extends GrantedAuthority> getAuthorities() {
                        return null;
                    }

                    @Override
                    public Object getCredentials() {
                        return null;
                    }

                    @Override
                    public Object getDetails() {
                        return null;
                    }

                    @Override
                    public Object getPrincipal() {
                        return new UserDetails(USER_ID, "", Collections.emptyList());
                    }

                    @Override
                    public boolean isAuthenticated() {
                        return false;
                    }

                    @Override
                    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

                    }

                    @Override
                    public String getName() {
                        return null;
                    }
                };
            }

            @Override
            public void setAuthentication(Authentication authentication) {

            }
        });
    }

    @Test
    public void shouldFindByUser() throws TechnicalException {
        final Token token2 = new Token();
        token2.setId("2");

        when(tokenRepository.findByReference(eq(USER.name()), eq(USER_ID))).thenReturn(asList(token, token2));

        final List<TokenEntity> tokens = tokenService.findByUser(USER_ID);

        assertEquals(TOKEN_ID, tokens.get(0).getId());
        assertEquals("name", tokens.get(0).getName());
        assertNull("Token cannot be read after creation", tokens.get(0).getToken());
        assertEquals(new Date(1486771200000L), tokens.get(0).getCreatedAt());
        assertEquals(new Date(1486772200000L), tokens.get(0).getExpiresAt());
        assertEquals(new Date(1486773200000L), tokens.get(0).getLastUseAt());
        assertEquals("2", tokens.get(1).getId());
    }

    @Test
    public void shouldFindByToken() throws TechnicalException {
        when(tokenRepository.findAll()).thenReturn(newHashSet(token));
        when(tokenRepository.update(token)).thenReturn(token);

        final Token t = tokenService.findByToken("token");

        assertEquals(TOKEN_ID, t.getId());
        assertEquals("name", t.getName());
        assertEquals("token", t.getToken());
        assertEquals(new Date(1486771200000L), t.getCreatedAt());
        assertEquals(new Date(1486772200000L), t.getExpiresAt());
        assertEquals(new Date(1486773200000L), t.getLastUseAt());
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        final NewTokenEntity newToken = new NewTokenEntity();
        newToken.setName("name");

        when(tokenRepository.create(any())).thenReturn(token);

        tokenService.create(newToken);

        verify(auditService).createPortalAuditLog(anyMap(), eq(TOKEN_CREATED), any(Date.class), isNull(), any());
        verify(tokenRepository).create(any());
        verify(tokenRepository).findByReference(eq(USER.name()), eq(USER_ID));
    }

    @Test(expected = TokenNameAlreadyExistsException.class)
    public void shouldNotCreateNameExists() throws TechnicalException {
        final NewTokenEntity newToken = new NewTokenEntity();
        newToken.setName("name");

        when(tokenRepository.findByReference(eq(USER.name()), eq(USER_ID))).thenReturn(singletonList(token));

        tokenService.create(newToken);
    }

    @Test
    public void shouldRevoke() throws TechnicalException {
        tokenService.revoke(TOKEN_ID);

        verify(auditService).createPortalAuditLog(anyMap(), eq(TOKEN_DELETED), any(Date.class), isNull(), eq(token));
        verify(tokenRepository).delete(TOKEN_ID);
    }

    @Test
    public void shouldRevokeByUser() throws TechnicalException {
        when(tokenRepository.findByReference(eq(USER.name()), eq(USER_ID))).thenReturn(singletonList(token));

        tokenService.revokeByUser(USER_ID);

        verify(auditService).createPortalAuditLog(anyMap(), eq(TOKEN_DELETED), any(Date.class), isNull(), eq(token));
        verify(tokenRepository).delete(TOKEN_ID);
    }
}
