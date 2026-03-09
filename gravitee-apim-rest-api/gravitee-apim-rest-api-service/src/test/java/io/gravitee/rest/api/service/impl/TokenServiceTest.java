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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Token.AuditEvent.TOKEN_CREATED;
import static io.gravitee.repository.management.model.Token.AuditEvent.TOKEN_DELETED;
import static io.gravitee.rest.api.model.TokenReferenceType.USER;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TokenRepository;
import io.gravitee.repository.management.model.Token;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.NewTokenEntity;
import io.gravitee.rest.api.model.TokenEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TokenNameAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.TokenNotFoundException;
import java.util.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
public class TokenServiceTest {

    private static final String USER_ID = "user123";
    private static final String TOKEN_ID = "1";
    private static final String CURRENT_ENVIRONMENT = "test";
    private static final String CURRENT_ORGANIZATION = "gravitee";

    @InjectMocks
    private final TokenService tokenService = new TokenServiceImpl();

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private AuditService auditService;

    Token token = Token.builder()
        .id(TOKEN_ID)
        .name("name")
        .token("token")
        .createdAt(new Date(1486771200000L))
        .expiresAt(new Date(1486772200000L))
        .lastUseAt(new Date(1486773200000L))
        .referenceId(USER_ID)
        .build();

    @Mock
    private PasswordEncoder passwordEncoder;

    @AfterAll
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
        GraviteeContext.cleanContext();
    }

    @BeforeEach
    public void init() throws TechnicalException {
        setField(tokenService, "passwordEncoder", passwordEncoder);
        lenient().when(passwordEncoder.matches(any(), any())).thenReturn(true);

        lenient().when(tokenRepository.findById(TOKEN_ID)).thenReturn(of(token));

        SecurityContextHolder.setContext(
            new SecurityContext() {
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
                        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

                        @Override
                        public String getName() {
                            return null;
                        }
                    };
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
        GraviteeContext.setCurrentOrganization(CURRENT_ORGANIZATION);
        GraviteeContext.setCurrentEnvironment(CURRENT_ENVIRONMENT);
    }

    @Test
    public void should_find_by_user() throws TechnicalException {
        final Token token2 = Token.builder().id("2").build();

        when(tokenRepository.findByReference(eq(USER.name()), eq(USER_ID))).thenReturn(List.of(token, token2));

        final List<TokenEntity> tokens = tokenService.findByUser(USER_ID);

        assertThat(tokens.getFirst().getId()).isEqualTo(TOKEN_ID);
        assertThat(tokens.getFirst().getName()).isEqualTo("name");
        assertThat(tokens.getFirst().getToken()).as("Token cannot be read after creation").isNull();
        assertThat(tokens.getFirst().getCreatedAt()).isEqualTo(new Date(1486771200000L));
        assertThat(tokens.getFirst().getExpiresAt()).isEqualTo(new Date(1486772200000L));
        assertThat(tokens.getFirst().getLastUseAt()).isEqualTo(new Date(1486773200000L));
        assertThat(tokens.get(1).getId()).isEqualTo("2");
    }

    @Test
    public void should_find_by_token() throws TechnicalException {
        when(tokenRepository.findAll()).thenReturn(Set.of(token));
        when(tokenRepository.update(token)).thenReturn(token);

        final Token t = tokenService.findByToken("token");

        assertThat(t.getId()).isEqualTo(TOKEN_ID);
    }

    @Test
    public void should_create() throws TechnicalException {
        final NewTokenEntity newToken = new NewTokenEntity();
        newToken.setName("name");

        when(tokenRepository.create(any())).thenReturn(token);

        tokenService.create(GraviteeContext.getExecutionContext(), newToken, USER_ID);

        verify(auditService).createOrganizationAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(auditLogData -> auditLogData.getEvent().equals(TOKEN_CREATED) && auditLogData.getOldValue() == null)
        );
        verify(tokenRepository).create(any());
        verify(tokenRepository).findByReference(eq(USER.name()), eq(USER_ID));
    }

    @Test
    public void should_not_create_if_name_already_exists() throws TechnicalException {
        final NewTokenEntity newToken = new NewTokenEntity();
        newToken.setName("name");

        when(tokenRepository.findByReference(eq(USER.name()), eq(USER_ID))).thenReturn(List.of(token));

        Throwable throwable = catchThrowable(() -> tokenService.create(GraviteeContext.getExecutionContext(), newToken, USER_ID));

        assertThat(throwable).isInstanceOf(TokenNameAlreadyExistsException.class);
    }

    @Test
    public void should_revoke() throws TechnicalException {
        tokenService.revoke(GraviteeContext.getExecutionContext(), TOKEN_ID);

        verify(auditService).createOrganizationAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getEvent().equals(TOKEN_DELETED) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue().equals(token)
            )
        );
        verify(tokenRepository).delete(TOKEN_ID);
    }

    @Test
    public void should_revoke_by_user() throws TechnicalException {
        when(tokenRepository.findByReference(eq(USER.name()), eq(USER_ID))).thenReturn(List.of(token));

        tokenService.revokeByUser(GraviteeContext.getExecutionContext(), USER_ID);

        verify(auditService).createOrganizationAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getEvent().equals(TOKEN_DELETED) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue().equals(token)
            )
        );
        verify(tokenRepository).delete(TOKEN_ID);
    }

    @Test
    public void findByToken_should_prioritize_last_used_token() throws TechnicalException {
        List<Token> tokens = List.of(
            Token.builder().lastUseAt(null).build(),
            Token.builder().lastUseAt(new Date(1486772200000L)).token("encodedToken1").build(),
            Token.builder().lastUseAt(new Date(1486772200999L)).token("encodedToken2").build(),
            Token.builder().lastUseAt(null).build()
        );

        when(tokenRepository.findAll()).thenReturn(new HashSet<>(tokens));
        doAnswer(returnsFirstArg()).when(tokenRepository).update(any());

        when(passwordEncoder.matches("inputToken", "encodedToken2")).thenReturn(false);
        when(passwordEncoder.matches("inputToken", "encodedToken1")).thenReturn(true);

        Token resultToken = tokenService.findByToken("inputToken");

        // Assert that token1 has been returned cause it's the one matching input token
        assertThat(resultToken).isEqualTo(tokens.get(1));

        // Assert that there was only 2 interactions with the passwordEncoder check, in this order :
        //   - first, token2 cause it's the most recently used
        //   - then, token1 cause it's the next most recently used
        // Token0 and token3 has not been checked
        InOrder inOrder = inOrder(passwordEncoder);
        inOrder.verify(passwordEncoder, times(1)).matches("inputToken", "encodedToken2");
        inOrder.verify(passwordEncoder, times(1)).matches("inputToken", "encodedToken1");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void findByToken_should_not_failed_if_matcher_failed() throws TechnicalException {
        // Given
        when(tokenRepository.findAll()).thenReturn(
            Set.of(Token.builder().id("failed").lastUseAt(new Date()).token("a").build(), Token.builder().token("b").build())
        );
        doAnswer(returnsFirstArg()).when(tokenRepository).update(any());

        when(passwordEncoder.matches(anyString(), eq("a"))).thenThrow(new RuntimeException("Mocked exception"));
        when(passwordEncoder.matches(anyString(), eq("b"))).thenReturn(true);

        // When
        Token resultToken = tokenService.findByToken("inputToken");

        // Then
        assertThat(resultToken.getToken()).isEqualTo("b");
    }

    @Test
    public void findByToken_should_throw_TokenNotFound_when_no_token_matches() throws TechnicalException {
        var tokens = Set.of(
            Token.builder().id(UUID.randomUUID().toString()).build(),
            Token.builder().id(UUID.randomUUID().toString()).build(),
            Token.builder().id(UUID.randomUUID().toString()).build(),
            Token.builder().id(UUID.randomUUID().toString()).build()
        );
        when(tokenRepository.findAll()).thenReturn(tokens);
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        Throwable throwable = catchThrowable(() -> tokenService.findByToken("inputToken"));
        // Assert that all 4 tokens have been checked using passwordEncoder
        // And none of them have been updated
        verify(passwordEncoder, times(4)).matches(any(), any());
        verify(tokenRepository, never()).update(any());
        assertThat(throwable).isInstanceOf(TokenNotFoundException.class);
    }

    @Test
    public void should_return_token_does_not_exist() throws TechnicalException {
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.empty());
        boolean tokenExistsForUser = tokenService.tokenExistsForUser(TOKEN_ID, USER_ID);
        assertThat(tokenExistsForUser).isFalse();
    }

    @Test
    public void should_return_token_does_not_exist_because_does_not_belong_to_user() {
        token.setReferenceId("another_user_id");
        boolean tokenExistsForUser = tokenService.tokenExistsForUser(TOKEN_ID, USER_ID);
        assertThat(tokenExistsForUser).isFalse();
    }

    @Test
    public void should_return_token_exists() {
        boolean tokenExistsForUser = tokenService.tokenExistsForUser(TOKEN_ID, USER_ID);
        assertThat(tokenExistsForUser).isTrue();
    }
}
