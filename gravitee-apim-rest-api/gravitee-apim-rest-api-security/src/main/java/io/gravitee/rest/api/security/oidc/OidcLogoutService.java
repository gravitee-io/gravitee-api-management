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

import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

@CustomLog
public class OidcLogoutService {

    private final CookieGenerator cookieGenerator;
    private final SocialIdentityProviderService socialIdentityProviderService;
    private final OidcIdTokenCookieCipher cipher;

    public OidcLogoutService(
        CookieGenerator cookieGenerator,
        Environment environment,
        SocialIdentityProviderService socialIdentityProviderService
    ) {
        this.cookieGenerator = cookieGenerator;
        this.socialIdentityProviderService = socialIdentityProviderService;
        this.cipher = new OidcIdTokenCookieCipher(OidcIdTokenCookieCipherSecrets.resolveRequired(environment));
    }

    public void storeOidcSession(HttpServletResponse response, String identityProviderId, String idToken) {
        if (!StringUtils.hasText(identityProviderId) || !StringUtils.hasText(idToken)) {
            return;
        }
        cipher
            .encrypt(idToken)
            .ifPresent(encrypted -> response.addCookie(cookieGenerator.generate(OidcCookieNames.ID_TOKEN_COOKIE, encrypted, true)));
        response.addCookie(cookieGenerator.generate(OidcCookieNames.IDENTITY_PROVIDER_COOKIE, identityProviderId, true));
    }

    public void clearOidcSession(HttpServletResponse response) {
        response.addCookie(cookieGenerator.generate(OidcCookieNames.ID_TOKEN_COOKIE, null));
        response.addCookie(cookieGenerator.generate(OidcCookieNames.IDENTITY_PROVIDER_COOKIE, null));
    }

    public Optional<OidcLogoutResult> performLogout(
        HttpServletRequest request,
        HttpServletResponse response,
        Cookie clearAuthCookie,
        IdentityProviderActivationService.ActivationTarget activationTarget,
        OidcLogoutPayload payload,
        Collection<String> allowedRedirectUriPrefixes
    ) {
        response.addCookie(clearAuthCookie);

        Optional<String> logoutUrl = buildLogoutUrl(request, activationTarget, payload, nonBlank(allowedRedirectUriPrefixes));
        clearOidcSession(response);

        return logoutUrl.map(url -> {
            OidcLogoutResult result = new OidcLogoutResult();
            result.setLogoutUrl(url);
            return result;
        });
    }

    public Optional<String> buildLogoutUrl(
        HttpServletRequest request,
        IdentityProviderActivationService.ActivationTarget activationTarget,
        OidcLogoutPayload payload,
        Collection<String> allowedRedirectUriPrefixes
    ) {
        String identityProviderId = payload != null && StringUtils.hasText(payload.getIdentityProviderId())
            ? payload.getIdentityProviderId()
            : readIdentityProviderId(request);
        if (!StringUtils.hasText(identityProviderId)) {
            return Optional.empty();
        }

        SocialIdentityProviderEntity identityProvider = socialIdentityProviderService.findById(identityProviderId, activationTarget);
        if (identityProvider == null || !StringUtils.hasText(identityProvider.getUserLogoutEndpoint())) {
            return Optional.empty();
        }

        String postLogoutRedirectUri = resolvePostLogoutRedirectUri(
            payload != null ? payload.getPostLogoutRedirectUri() : null,
            allowedRedirectUriPrefixes
        );
        String idTokenHint = readIdTokenHint(request);

        return Optional.of(
            buildEndSessionUrl(identityProvider.getUserLogoutEndpoint(), identityProvider.getClientId(), idTokenHint, postLogoutRedirectUri)
        );
    }

    private String readCookieValue(HttpServletRequest request, String cookieName) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        return cookie != null && StringUtils.hasText(cookie.getValue()) ? cookie.getValue() : null;
    }

    private String readIdentityProviderId(HttpServletRequest request) {
        return readCookieValue(request, OidcCookieNames.IDENTITY_PROVIDER_COOKIE);
    }

    private String readIdTokenHint(HttpServletRequest request) {
        String encryptedValue = readCookieValue(request, OidcCookieNames.ID_TOKEN_COOKIE);
        if (encryptedValue == null) {
            return null;
        }
        return cipher.decrypt(encryptedValue).orElse(null);
    }

    private String resolvePostLogoutRedirectUri(String requestedUri, Collection<String> allowedRedirectUriPrefixes) {
        if (StringUtils.hasText(requestedUri) && isAllowedRedirectUri(requestedUri, allowedRedirectUriPrefixes)) {
            return requestedUri;
        }
        if (StringUtils.hasText(requestedUri)) {
            log.warn("Rejected post_logout_redirect_uri: {}", requestedUri);
        }
        return allowedRedirectUriPrefixes.stream().filter(StringUtils::hasText).findFirst().orElse(null);
    }

    static boolean isAllowedRedirectUri(String redirectUri, Collection<String> allowedPrefixes) {
        if (!StringUtils.hasText(redirectUri) || allowedPrefixes == null || allowedPrefixes.isEmpty()) {
            return false;
        }
        try {
            String candidateOrigin = toOrigin(redirectUri.trim());
            if (candidateOrigin == null) {
                return false;
            }
            for (String allowed : allowedPrefixes) {
                if (!StringUtils.hasText(allowed)) {
                    continue;
                }
                String allowedOrigin = toOrigin(allowed.trim());
                if (allowedOrigin != null && candidateOrigin.equalsIgnoreCase(allowedOrigin)) {
                    return true;
                }
            }
        } catch (IllegalArgumentException e) {
            log.debug("Invalid post_logout_redirect_uri: {}", redirectUri, e);
        }
        return false;
    }

    private static String toOrigin(String uri) {
        URI parsed = URI.create(uri);
        if (parsed.getScheme() == null || parsed.getAuthority() == null) {
            return null;
        }
        return parsed.getScheme() + "://" + parsed.getAuthority();
    }

    static String buildEndSessionUrl(String logoutEndpoint, String clientId, String idTokenHint, String postLogoutRedirectUri) {
        StringBuilder url = new StringBuilder(logoutEndpoint);
        char separator = logoutEndpoint.contains("?") ? '&' : '?';

        if (StringUtils.hasText(idTokenHint)) {
            url.append(separator).append("id_token_hint=").append(encode(idTokenHint));
            separator = '&';
        }
        if (StringUtils.hasText(clientId)) {
            url.append(separator).append("client_id=").append(encode(clientId));
            separator = '&';
        }
        if (StringUtils.hasText(postLogoutRedirectUri)) {
            url.append(separator).append("post_logout_redirect_uri=").append(encode(postLogoutRedirectUri));
        }
        return url.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static List<String> nonBlank(Collection<String> values) {
        return values == null ? List.of() : values.stream().filter(StringUtils::hasText).toList();
    }
}
