package io.gravitee.gamma.module.authz.entityimport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.gamma.module.authz.entityimport.repository.ScimConnectorDocument;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ScimTokenProvider}.
 *
 * <p>The provider holds an {@link HttpClient} bound to the OAuth2 token endpoint
 * and has no seam to mock the network call, so the suite spins an embedded
 * {@link HttpServer} on {@code 127.0.0.1:0} and points the provider at it. The
 * server records form bodies + auth headers so we can assert the grant payload
 * matches RFC 6749 §6 (grant_type, refresh_token, client_id semantics, Basic
 * auth header) without hand-rolling an OAuth2 mock.
 */
@ExtendWith(MockitoExtension.class)
class ScimTokenProviderTest {

    @Mock
    private DataEncryptor dataEncryptor;

    private HttpServer server;
    private String tokenUrl;
    private ScimTokenProvider provider;

    private final List<String> requestBodies = new ArrayList<>();
    private final List<String> authHeaders = new ArrayList<>();
    private volatile int responseStatus = 200;
    private volatile String responseBody = "{ \"access_token\": \"new-access\", \"token_type\": \"Bearer\", \"expires_in\": 3600 }";

    @BeforeEach
    void start() throws IOException, GeneralSecurityException {
        lenient()
            .when(dataEncryptor.encrypt(anyString()))
            .thenAnswer(inv -> "enc:" + inv.getArgument(0));
        lenient()
            .when(dataEncryptor.decrypt(anyString()))
            .thenAnswer(inv -> {
                String s = inv.getArgument(0);
                return s.startsWith("enc:") ? s.substring(4) : s;
            });

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        HttpHandler handler = exchange -> {
            authHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        };
        server.createContext("/oauth/token", handler);
        server.start();
        tokenUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/oauth/token";

        provider = new ScimTokenProvider(dataEncryptor, HttpClient.newHttpClient());
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    private ScimConnectorDocument staticDoc(String encryptedToken) {
        ScimConnectorDocument d = new ScimConnectorDocument();
        d.setEnvironmentId("env-1");
        d.setName("okta");
        d.setUrl("https://idp/scim");
        d.setToken(encryptedToken);
        return d;
    }

    private ScimConnectorDocument refreshDoc() {
        ScimConnectorDocument d = new ScimConnectorDocument();
        d.setEnvironmentId("env-1");
        d.setName("okta");
        d.setUrl("https://idp/scim");
        d.setTokenUrl(tokenUrl);
        d.setClientId("client-abc");
        d.setClientSecret("enc:secret-xyz");
        d.setRefreshToken("enc:refresh-1");
        return d;
    }

    // ─────────────────────────────────────────────────────────────────────
    // static mode — no token endpoint involved
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void resolve_returnsNull_whenNoTokenConfigured() {
        ScimConnectorDocument doc = new ScimConnectorDocument();

        assertThat(provider.resolve(doc)).isNull();
        assertThat(requestBodies).isEmpty();
    }

    @Test
    void resolve_returnsDecryptedStaticToken_whenOnlyTokenSet() {
        assertThat(provider.resolve(staticDoc("enc:static-tok"))).isEqualTo("static-tok");
        assertThat(requestBodies).as("static mode must not call token endpoint").isEmpty();
    }

    @Test
    void resolve_returnsDecryptedStaticToken_whenTokenUrlSetButNoRefreshToken() {
        // tokenUrl without refresh_token is not a valid refresh-mode config —
        // fall back to the static `token` so half-configured connectors don't
        // silently break.
        ScimConnectorDocument doc = staticDoc("enc:static-tok");
        doc.setTokenUrl(tokenUrl);

        assertThat(provider.resolve(doc)).isEqualTo("static-tok");
        assertThat(requestBodies).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────
    // refresh mode — happy path + caching
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void resolve_callsTokenEndpoint_withRefreshTokenGrant_whenCacheMissing() {
        Instant before = Instant.now();
        ScimConnectorDocument doc = refreshDoc();

        String accessToken = provider.resolve(doc);

        assertThat(accessToken).isEqualTo("new-access");
        assertThat(requestBodies).hasSize(1);
        assertThat(requestBodies.get(0)).contains("grant_type=refresh_token").contains("refresh_token=refresh-1");
        assertThat(doc.getAccessToken()).isEqualTo("enc:new-access");
        assertThat(doc.getAccessTokenExpiresAt()).isAfter(before.plusSeconds(3599)).isBefore(before.plusSeconds(3700));
    }

    @Test
    void resolve_sendsClientSecretBasicAuth_whenClientIdAndSecretSet() {
        provider.resolve(refreshDoc());

        assertThat(authHeaders).hasSize(1);
        String expected = "Basic " + Base64.getEncoder().encodeToString("client-abc:secret-xyz".getBytes(StandardCharsets.UTF_8));
        assertThat(authHeaders.get(0)).isEqualTo(expected);
        // client_id MUST NOT also be in the body when Basic auth carries it,
        // per RFC 6749 §2.3.1.
        assertThat(requestBodies.get(0)).doesNotContain("client_id=");
    }

    @Test
    void resolve_putsClientIdInBody_whenClientSecretIsAbsent() {
        // Public-client style: client_id but no secret. The id goes in the
        // body and there's no Authorization header.
        ScimConnectorDocument doc = refreshDoc();
        doc.setClientSecret(null);

        provider.resolve(doc);

        assertThat(authHeaders).containsExactly((String) null);
        assertThat(requestBodies.get(0)).contains("client_id=client-abc");
    }

    @Test
    void resolve_reusesCachedAccessToken_whenStillValid() {
        ScimConnectorDocument doc = refreshDoc();
        doc.setAccessToken("enc:cached-access");
        doc.setAccessTokenExpiresAt(Instant.now().plusSeconds(1800)); // 30 min ahead

        String accessToken = provider.resolve(doc);

        assertThat(accessToken).isEqualTo("cached-access");
        assertThat(requestBodies).as("cached token still valid — must not hit token endpoint").isEmpty();
    }

    @Test
    void resolve_refreshesEarly_whenAccessTokenExpiresWithinSafetyMargin() {
        // 30s ahead < REFRESH_SAFETY_MARGIN (60s) → must refresh now instead
        // of risking expiry mid-sync.
        ScimConnectorDocument doc = refreshDoc();
        doc.setAccessToken("enc:about-to-expire");
        doc.setAccessTokenExpiresAt(Instant.now().plusSeconds(30));

        String accessToken = provider.resolve(doc);

        assertThat(accessToken).isEqualTo("new-access");
        assertThat(requestBodies).hasSize(1);
    }

    @Test
    void resolve_refreshes_whenCachedAccessTokenIsAlreadyExpired() {
        ScimConnectorDocument doc = refreshDoc();
        doc.setAccessToken("enc:expired");
        doc.setAccessTokenExpiresAt(Instant.now().minusSeconds(60));

        assertThat(provider.resolve(doc)).isEqualTo("new-access");
        assertThat(requestBodies).hasSize(1);
    }

    // ─────────────────────────────────────────────────────────────────────
    // refresh mode — refresh token rotation
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void resolve_rotatesRefreshToken_whenIdpReturnsNewOne() {
        responseBody = "{ \"access_token\": \"a1\", \"expires_in\": 3600, \"refresh_token\": \"rotated-refresh\" }";
        ScimConnectorDocument doc = refreshDoc();

        provider.resolve(doc);

        assertThat(doc.getRefreshToken()).isEqualTo("enc:rotated-refresh");
    }

    @Test
    void resolve_keepsExistingRefreshToken_whenIdpDoesNotRotate() {
        responseBody = "{ \"access_token\": \"a1\", \"expires_in\": 3600 }";
        ScimConnectorDocument doc = refreshDoc();

        provider.resolve(doc);

        assertThat(doc.getRefreshToken()).isEqualTo("enc:refresh-1");
    }

    @Test
    void resolve_skipsBlankRefreshTokenInResponse() {
        responseBody = "{ \"access_token\": \"a1\", \"expires_in\": 3600, \"refresh_token\": \"   \" }";
        ScimConnectorDocument doc = refreshDoc();

        provider.resolve(doc);

        assertThat(doc.getRefreshToken()).isEqualTo("enc:refresh-1");
    }

    // ─────────────────────────────────────────────────────────────────────
    // refresh mode — error handling
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void resolve_throwsTokenRefreshException_onHttp4xx() {
        responseStatus = 401;
        responseBody = "{ \"error\": \"invalid_grant\" }";

        assertThatThrownBy(() -> provider.resolve(refreshDoc()))
            .isInstanceOf(ScimTokenProvider.TokenRefreshException.class)
            .hasMessageContaining("HTTP 401");
    }

    @Test
    void resolve_throwsTokenRefreshException_onMissingAccessTokenInResponse() {
        responseBody = "{ \"token_type\": \"Bearer\", \"expires_in\": 3600 }";

        assertThatThrownBy(() -> provider.resolve(refreshDoc()))
            .isInstanceOf(ScimTokenProvider.TokenRefreshException.class)
            .hasMessageContaining("access_token");
    }

    @Test
    void resolve_throwsTokenRefreshException_onInvalidJson() {
        responseBody = "<<not json>>";

        assertThatThrownBy(() -> provider.resolve(refreshDoc()))
            .isInstanceOf(ScimTokenProvider.TokenRefreshException.class)
            .hasMessageContaining("invalid JSON");
    }

    @Test
    void resolve_throwsTokenRefreshException_whenTokenEndpointUnreachable() {
        ScimConnectorDocument doc = refreshDoc();
        // Port 1 is reserved / will refuse connections on every reasonable host.
        doc.setTokenUrl("http://127.0.0.1:1/oauth/token");

        assertThatThrownBy(() -> provider.resolve(doc))
            .isInstanceOf(ScimTokenProvider.TokenRefreshException.class)
            .hasMessageContaining("Failed to call token endpoint");
    }

    @Test
    void resolve_defaultsExpiresInTo3600s_whenIdpOmitsIt() {
        responseBody = "{ \"access_token\": \"a1\" }";
        ScimConnectorDocument doc = refreshDoc();
        Instant before = Instant.now();

        provider.resolve(doc);

        assertThat(doc.getAccessTokenExpiresAt()).isAfter(before.plusSeconds(3590)).isBefore(before.plusSeconds(3700));
    }
}
