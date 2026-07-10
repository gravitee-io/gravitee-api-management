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

import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
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
    }

    @Test
    void should_perform_logout_without_idp_redirect_when_no_provider_is_known() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("jwt.secret", "test-secret");
        OidcLogoutService service = new OidcLogoutService(cookieGenerator, environment, socialIdentityProviderService);
        Cookie clearAuthCookie = new Cookie("Auth-Graviteeio-APIM", null);

        Optional<OidcLogoutResult> result = service.performLogout(request, response, clearAuthCookie, null, null, "http://localhost:4100");

        assertThat(result).isEmpty();
    }
}
