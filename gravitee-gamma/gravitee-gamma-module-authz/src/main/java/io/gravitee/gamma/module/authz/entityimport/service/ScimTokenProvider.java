package io.gravitee.gamma.module.authz.entityimport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.gamma.module.authz.entityimport.repository.ScimConnectorDocument;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Resolves the bearer token used for SCIM calls, transparently refreshing it
 * via OAuth2 refresh_token grant (RFC 6749 §6) when the connector is configured
 * with a {@code tokenUrl} + {@code refreshToken}.
 *
 * <p>Two modes are supported:
 * <ul>
 *   <li><b>static</b> — only {@code token} is set; the value is decrypted and
 *       returned as-is (legacy behaviour).</li>
 *   <li><b>refresh</b> — {@code tokenUrl} and {@code refreshToken} are set;
 *       the provider keeps a cached access token on the document along with
 *       its expiry, and refreshes it just in time.</li>
 * </ul>
 *
 * <p>The provider mutates the document in place (sets {@code accessToken},
 * {@code accessTokenExpiresAt}, and possibly rotates {@code refreshToken});
 * the caller is responsible for persisting the document after a successful
 * sync so the rotated token isn't lost.
 *
 * <p>A {@link #REFRESH_SAFETY_MARGIN} is subtracted from the upstream's
 * {@code expires_in} so a token that's about to expire mid-sync is refreshed
 * preemptively instead of failing the SCIM call halfway through pagination.
 */
@Service
public class ScimTokenProvider {

    static final Duration REFRESH_SAFETY_MARGIN = Duration.ofSeconds(60);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DataEncryptor dataEncryptor;
    private final HttpClient http;

    @Autowired
    public ScimTokenProvider(DataEncryptor dataEncryptor) {
        this(dataEncryptor, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    ScimTokenProvider(DataEncryptor dataEncryptor, HttpClient http) {
        this.dataEncryptor = dataEncryptor;
        this.http = http;
    }

    public String resolve(ScimConnectorDocument doc) {
        if (isRefreshMode(doc)) {
            if (cachedAccessTokenStillValid(doc)) {
                return decrypt(doc.getAccessToken());
            }
            return refresh(doc);
        }
        return decrypt(doc.getToken());
    }

    private static boolean isRefreshMode(ScimConnectorDocument doc) {
        return notBlank(doc.getTokenUrl()) && notBlank(doc.getRefreshToken());
    }

    private static boolean cachedAccessTokenStillValid(ScimConnectorDocument doc) {
        if (!notBlank(doc.getAccessToken()) || doc.getAccessTokenExpiresAt() == null) return false;
        return doc.getAccessTokenExpiresAt().isAfter(Instant.now().plus(REFRESH_SAFETY_MARGIN));
    }

    private String refresh(ScimConnectorDocument doc) {
        String refreshTokenPlain = decrypt(doc.getRefreshToken());
        StringBuilder form = new StringBuilder("grant_type=refresh_token&refresh_token=").append(urlEncode(refreshTokenPlain));
        if (notBlank(doc.getClientId()) && !notBlank(doc.getClientSecret())) {
            form.append("&client_id=").append(urlEncode(doc.getClientId()));
        }

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(doc.getTokenUrl()))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form.toString(), StandardCharsets.UTF_8));
        if (notBlank(doc.getClientId()) && notBlank(doc.getClientSecret())) {
            String credentials = doc.getClientId() + ":" + decrypt(doc.getClientSecret());
            b.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }

        HttpResponse<String> resp;
        try {
            resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new TokenRefreshException("Failed to call token endpoint: " + e.getMessage(), e);
        }
        if (resp.statusCode() >= 400) {
            throw new TokenRefreshException("Token endpoint returned HTTP " + resp.statusCode());
        }
        JsonNode body;
        try {
            body = MAPPER.readTree(resp.body());
        } catch (Exception e) {
            throw new TokenRefreshException("Token endpoint returned invalid JSON: " + e.getMessage(), e);
        }
        JsonNode access = body.get("access_token");
        if (access == null || access.isNull() || access.asText().isBlank()) {
            throw new TokenRefreshException("Token endpoint response missing access_token");
        }
        String accessTokenPlain = access.asText();
        long expiresInSeconds = body.path("expires_in").asLong(3600L);
        doc.setAccessToken(encrypt(accessTokenPlain));
        doc.setAccessTokenExpiresAt(Instant.now().plusSeconds(expiresInSeconds));

        JsonNode rotated = body.get("refresh_token");
        if (rotated != null && !rotated.isNull() && !rotated.asText().isBlank()) {
            doc.setRefreshToken(encrypt(rotated.asText()));
        }
        return accessTokenPlain;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String encrypt(String plaintext) {
        try {
            return dataEncryptor.encrypt(plaintext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt SCIM token", e);
        }
    }

    private String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) return null;
        try {
            return dataEncryptor.decrypt(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt SCIM token", e);
        }
    }

    public static class TokenRefreshException extends RuntimeException {

        public TokenRefreshException(String message) {
            super(message);
        }

        public TokenRefreshException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
