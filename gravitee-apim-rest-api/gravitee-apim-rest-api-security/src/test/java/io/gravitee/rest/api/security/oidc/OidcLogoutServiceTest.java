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
package io.gravitee.rest.api.security.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
class OidcLogoutServiceTest {

    @Mock
    private CookieGenerator cookieGenerator;

    @Mock
    private SocialIdentityProviderService socialIdentityProviderService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private OidcLogoutService service;
    private IdentityProviderActivationService.ActivationTarget activationTarget;

    @BeforeEach
    void setUp() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("jwt.secret", "test-secret");
        service = new OidcLogoutService(cookieGenerator, environment, socialIdentityProviderService);
        activationTarget = new IdentityProviderActivationService.ActivationTarget(
            "DEFAULT",
            io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType.ENVIRONMENT
        );
    }

    @Test
    void should_build_end_session_url_with_id_token_hint() {
        String url = OidcLogoutService.buildEndSessionUrl(
            "https://idp.example.com/logout",
            "apim-client",
            "id-token-value",
            "http://localhost:4100/"
        );

        assertThat(url).contains("id_token_hint=id-token-value");
        assertThat(url).contains("client_id=apim-client");
        assertThat(url).contains("post_logout_redirect_uri=");
    }

    @Test
    void should_validate_post_logout_redirect_uri() {
        assertThat(OidcLogoutService.isAllowedRedirectUri("http://localhost:4100/home", List.of("http://localhost:4100"))).isTrue();
        assertThat(OidcLogoutService.isAllowedRedirectUri("https://evil.example.com/", List.of("http://localhost:4100"))).isFalse();
        assertThat(OidcLogoutService.isAllowedRedirectUri(null, List.of("http://localhost:4100"))).isFalse();
        assertThat(OidcLogoutService.isAllowedRedirectUri("http://localhost:4100/", List.of())).isFalse();
        assertThat(OidcLogoutService.nonBlank(null)).isEmpty();
        assertThat(OidcLogoutService.nonBlank(List.of(" ", "http://localhost:4100"))).containsExactly("http://localhost:4100");
    }

    @Test
    void should_perform_logout_without_idp_redirect_when_no_provider_is_known() {
        Cookie clearAuthCookie = new Cookie("Auth-Graviteeio-APIM", null);

        Optional<OidcLogoutResult> result = service.performLogout(
            request,
            response,
            clearAuthCookie,
            null,
            null,
            List.of("http://localhost:4100")
        );

        assertThat(result).isEmpty();
        verify(response).addCookie(clearAuthCookie);
    }

    @Test
    void should_store_and_clear_oidc_session_cookies() {
        when(cookieGenerator.generate(eq(OidcCookieNames.ID_TOKEN_COOKIE), any(), eq(true))).thenReturn(
            new Cookie(OidcCookieNames.ID_TOKEN_COOKIE, "encrypted")
        );
        when(cookieGenerator.generate(eq(OidcCookieNames.IDENTITY_PROVIDER_COOKIE), eq("google"), eq(true))).thenReturn(
            new Cookie(OidcCookieNames.IDENTITY_PROVIDER_COOKIE, "google")
        );
        when(cookieGenerator.generate(OidcCookieNames.ID_TOKEN_COOKIE, null)).thenReturn(new Cookie(OidcCookieNames.ID_TOKEN_COOKIE, null));
        when(cookieGenerator.generate(OidcCookieNames.IDENTITY_PROVIDER_COOKIE, null)).thenReturn(
            new Cookie(OidcCookieNames.IDENTITY_PROVIDER_COOKIE, null)
        );

        service.storeOidcSession(response, "google", "some.id.token");
        verify(response, org.mockito.Mockito.times(2)).addCookie(any());

        service.clearOidcSession(response);
        verify(response, org.mockito.Mockito.times(4)).addCookie(any());
    }

    @Test
    void should_ignore_store_oidc_session_when_values_are_blank() {
        service.storeOidcSession(response, null, "token");
        service.storeOidcSession(response, "google", " ");
        verify(response, org.mockito.Mockito.never()).addCookie(any());
    }

    @Test
    void should_build_logout_url_from_payload_and_allowed_redirect() {
        SocialIdentityProviderEntity identityProvider = mock(SocialIdentityProviderEntity.class);
        when(identityProvider.getClientId()).thenReturn("apim-client");
        when(identityProvider.getUserLogoutEndpoint()).thenReturn("https://idp.example.com/logout");
        when(socialIdentityProviderService.findById("google", activationTarget)).thenReturn(identityProvider);

        OidcLogoutPayload payload = new OidcLogoutPayload();
        payload.setIdentityProviderId("google");
        payload.setPostLogoutRedirectUri("http://localhost:4100/home");

        Optional<String> logoutUrl = service.buildLogoutUrl(request, activationTarget, payload, List.of("http://localhost:4100"));

        assertThat(logoutUrl).isPresent();
        assertThat(logoutUrl.get()).contains("https://idp.example.com/logout");
        assertThat(logoutUrl.get()).contains("client_id=apim-client");
        assertThat(logoutUrl.get()).contains("post_logout_redirect_uri=");
    }

    @Test
    void should_build_logout_url_using_cookies_when_payload_has_no_provider() {
        SocialIdentityProviderEntity identityProvider = mock(SocialIdentityProviderEntity.class);
        when(identityProvider.getClientId()).thenReturn("apim-client");
        when(identityProvider.getUserLogoutEndpoint()).thenReturn("https://idp.example.com/logout");
        when(socialIdentityProviderService.findById("google", activationTarget)).thenReturn(identityProvider);

        OidcIdTokenCookieCipher cipher = new OidcIdTokenCookieCipher("test-secret");
        String encrypted = cipher.encrypt("id-token-hint-value").orElseThrow();
        when(request.getCookies()).thenReturn(
            new Cookie[] {
                new Cookie(OidcCookieNames.IDENTITY_PROVIDER_COOKIE, "google"),
                new Cookie(OidcCookieNames.ID_TOKEN_COOKIE, encrypted),
            }
        );

        Optional<String> logoutUrl = service.buildLogoutUrl(null, activationTarget, null, List.of("http://localhost:4100"));
        assertThat(logoutUrl).isEmpty();

        logoutUrl = service.buildLogoutUrl(request, activationTarget, null, List.of("http://localhost:4100"));
        assertThat(logoutUrl).isPresent();
        assertThat(logoutUrl.get()).contains("id_token_hint=");
    }

    @Test
    void should_reject_disallowed_post_logout_redirect_and_fallback_to_configured_url() {
        SocialIdentityProviderEntity identityProvider = mock(SocialIdentityProviderEntity.class);
        when(identityProvider.getClientId()).thenReturn("apim-client");
        when(identityProvider.getUserLogoutEndpoint()).thenReturn("https://idp.example.com/logout");
        when(socialIdentityProviderService.findById("google", activationTarget)).thenReturn(identityProvider);

        OidcLogoutPayload payload = new OidcLogoutPayload();
        payload.setIdentityProviderId("google");
        payload.setPostLogoutRedirectUri("https://evil.example.com/phish");

        Optional<String> logoutUrl = service.buildLogoutUrl(request, activationTarget, payload, List.of("http://localhost:4100"));

        assertThat(logoutUrl).isPresent();
        assertThat(logoutUrl.get()).contains("post_logout_redirect_uri=http%3A%2F%2Flocalhost%3A4100");
        assertThat(logoutUrl.get()).doesNotContain("evil.example.com");
    }

    @Test
    void should_fail_to_start_when_no_encryption_secret_is_configured() {
        assertThatThrownBy(() -> new OidcLogoutService(cookieGenerator, new MockEnvironment(), socialIdentityProviderService))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(OidcIdTokenCookieCipherSecrets.DEDICATED_SECRET_PROPERTY);
    }
}
