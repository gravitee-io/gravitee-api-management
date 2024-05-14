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
package io.gravitee.integration.controller.websocket.auth;

import static io.gravitee.integration.controller.websocket.auth.IntegrationWebsocketControllerAuthentication.AUTHORIZATION_HEADER_BEARER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.core.model.LicenseFixtures;
import inmemory.LicenseCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.integration.controller.command.IntegrationCommandContext;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.node.license.DefaultLicenseManager;
import io.gravitee.repository.management.model.Token;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.exceptions.TokenNotFoundException;
import io.vertx.rxjava3.core.MultiMap;
import io.vertx.rxjava3.core.http.HttpHeaders;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationWebsocketControllerAuthenticationTest {

    private static final String TOKEN_VALUE = "my-token-value";
    private static final String ORGANIZATION_ID = "organization-id";

    @Mock
    TokenService tokenService;

    @Mock
    HttpServerRequest request;

    LicenseManager licenseManager = mock(LicenseManager.class);
    UserCrudServiceInMemory userCrudServiceInMemory = new UserCrudServiceInMemory();

    IntegrationWebsocketControllerAuthentication authentication;

    @BeforeEach
    void setUp() {
        authentication =
            new IntegrationWebsocketControllerAuthentication(
                tokenService,
                userCrudServiceInMemory,
                new LicenseDomainService(new LicenseCrudServiceInMemory(), licenseManager)
            );

        MultiMap requestHeaders = HttpHeaders.headers();
        requestHeaders
            .add(IntegrationWebsocketControllerAuthentication.AUTHORIZATION_HEADER, AUTHORIZATION_HEADER_BEARER + TOKEN_VALUE)
            .add(IntegrationWebsocketControllerAuthentication.ORGANIZATION_HEADER, ORGANIZATION_ID);
        lenient().when(request.headers()).thenReturn(requestHeaders);

        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anEnterpriseLicense());
    }

    @AfterEach
    void tearDown() {
        userCrudServiceInMemory.reset();
        reset(licenseManager);
    }

    @Test
    void should_return_a_valid_IntegrationCommandContext_when_authentication_succeed() {
        var token = givenToken(Token.builder().token(TOKEN_VALUE).referenceId("user-id").build());
        givenUser(BaseUserEntity.builder().id(token.getReferenceId()).organizationId(ORGANIZATION_ID).build());

        var result = authentication.authenticate(request);

        assertThat(result).isEqualTo(new IntegrationCommandContext(true, ORGANIZATION_ID));
    }

    @Test
    void should_return_an_invalid_IntegrationCommandContext_when_no_token_found() {
        givenNoToken();

        var result = authentication.authenticate(request);

        assertThat(result).isEqualTo(new IntegrationCommandContext(false));
    }

    @Test
    void should_return_an_invalid_IntegrationCommandContext_when_no_user_found() {
        givenToken(Token.builder().token(TOKEN_VALUE).referenceId("user-id").build());

        var result = authentication.authenticate(request);

        assertThat(result).isEqualTo(new IntegrationCommandContext(false));
    }

    @Test
    void should_return_an_invalid_IntegrationCommandContext_when_no_authorization_headers() {
        lenient().when(request.headers()).thenReturn(HttpHeaders.headers());

        var result = authentication.authenticate(request);

        assertThat(result).isEqualTo(new IntegrationCommandContext(false));
    }

    @Test
    void should_return_an_invalid_IntegrationCommandContext_when_no_enterprise_license_found() {
        var token = givenToken(Token.builder().token(TOKEN_VALUE).referenceId("user-id").build());
        givenUser(BaseUserEntity.builder().id(token.getReferenceId()).organizationId(ORGANIZATION_ID).build());
        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anOssLicense());

        var result = authentication.authenticate(request);

        assertThat(result).isEqualTo(new IntegrationCommandContext(false));
    }

    private Token givenToken(Token token) {
        lenient().when(tokenService.findByToken(token.getToken())).thenReturn(token);
        return token;
    }

    private void givenNoToken() {
        lenient().when(tokenService.findByToken(any())).thenThrow(new TokenNotFoundException("token"));
    }

    private void givenUser(BaseUserEntity user) {
        userCrudServiceInMemory.initWith(List.of(user));
    }
}
