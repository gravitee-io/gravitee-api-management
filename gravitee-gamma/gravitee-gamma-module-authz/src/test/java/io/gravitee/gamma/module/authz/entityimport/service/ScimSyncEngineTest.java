package io.gravitee.gamma.module.authz.entityimport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.gravitee.apim.authorization.api.AuthzCallerContext;
import io.gravitee.apim.authorization.api.EntityAdminApi;
import io.gravitee.apim.authorization.domain.Entity;
import io.gravitee.apim.authorization.domain.EntityKind;
import io.gravitee.apim.authorization.service.CreateOrReplaceEntityCommand;
import io.gravitee.apim.authorization.service.EntityFilter;
import io.gravitee.apim.authorization.service.UpsertResult;
import io.gravitee.gamma.module.authz.entityimport.repository.ScimConnectorDocument;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ScimSyncEngine}.
 *
 * <p>Two concerns are covered:
 * <ul>
 *   <li><b>Pure helper</b> ({@link ScimSyncEngine#entityIdFor}) — string function,
 *       no I/O, table-driven.</li>
 *   <li><b>sync()</b> — exercised end-to-end against an embedded
 *       {@link HttpServer} bound to {@code 127.0.0.1:0}. The engine instantiates
 *       its own {@link java.net.http.HttpClient} in the constructor and doesn't
 *       expose a seam to mock it, so spinning a real loopback server (~5ms) is
 *       the cheapest way to assert the SCIM payload parsing too.</li>
 * </ul>
 *
 * <p>Methods are named {@code entityIdFor_*} / {@code sync_*} rather than grouped
 * via {@code @Nested}: surefire 3.5.5 + the current gravitee-parent inclusion
 * pattern doesn't discover nested classes, so they'd run zero tests silently.
 */
@ExtendWith(MockitoExtension.class)
class ScimSyncEngineTest {

    @Mock
    private EntityAdminApi entityApi;

    private ScimSyncEngine engine;
    private HttpServer server;
    private String baseUrl;
    /** Static fixture body, returned regardless of query string. */
    private final Map<String, String> responses = new HashMap<>();
    /** Dynamic fixture per path: given the full request URI, return the body. Wins over `responses`. */
    private final Map<String, Function<URI, String>> dynamicResponses = new HashMap<>();
    private final List<String> requestedPaths = new ArrayList<>();
    /** Full request URIs in fetch order — for asserting pagination query strings. */
    private final List<String> requestedQueries = new ArrayList<>();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        HttpHandler handler = exchange -> {
            URI uri = exchange.getRequestURI();
            requestedPaths.add(uri.getPath());
            requestedQueries.add(uri.getQuery() == null ? "" : uri.getQuery());
            Function<URI, String> dynamic = dynamicResponses.get(uri.getPath());
            String body = dynamic != null ? dynamic.apply(uri) : responses.getOrDefault(uri.getPath(), "{\"Resources\":[]}");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/scim+json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        };
        server.createContext("/Users", handler);
        server.createContext("/Groups", handler);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        engine = new ScimSyncEngine(entityApi);
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    private ScimConnectorDocument connector(boolean importUsers, boolean importGroups) {
        ScimConnectorDocument d = new ScimConnectorDocument();
        d.setId("conn-1");
        d.setEnvironmentId("env-1");
        d.setName("okta");
        d.setUrl(baseUrl);
        d.setToken(null);
        d.setImportUsers(importUsers);
        d.setImportGroups(importGroups);
        return d;
    }

    private static UpsertResult upsertResult(String entityId, boolean created) {
        Entity entity = new Entity(
            "row-" + entityId,
            entityId,
            EntityKind.PRINCIPAL,
            Map.of(),
            List.of(),
            "scim",
            "env-1",
            Instant.now(),
            Instant.now()
        );
        return new UpsertResult(entity, created);
    }

    private static Entity scimEntity(String entityId, String connectorName) {
        return new Entity(
            "row-" + entityId,
            entityId,
            EntityKind.PRINCIPAL,
            Map.of("_connector", connectorName, "_kind", entityId.split("\\.")[0]),
            List.of(),
            "scim",
            "env-1",
            Instant.now(),
            Instant.now()
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // Pagination fixture helpers
    // ─────────────────────────────────────────────────────────────────────

    private static int parseStartIndex(URI uri) {
        String q = uri.getQuery();
        if (q == null) return 1;
        for (String part : q.split("&")) {
            if (part.startsWith("startIndex=")) {
                return Integer.parseInt(part.substring("startIndex=".length()));
            }
        }
        return 1;
    }

    /**
     * Synthesizes a SCIM /Users page response for a server containing
     * {@code total} users; the returned body covers indices
     * {@code [startIndex, startIndex + pageSize)} clipped to {@code total}.
     */
    private static String usersPage(URI uri, int total, int pageSize) {
        int startIndex = parseStartIndex(uri);
        int from = startIndex;
        int to = Math.min(startIndex + pageSize - 1, total);
        if (from > total) {
            return usersBody(List.of(), total);
        }
        return usersBody(buildUsers(from, to), total);
    }

    private static List<String> buildUsers(int from, int to) {
        List<String> users = new ArrayList<>(to - from + 1);
        for (int i = from; i <= to; i++) {
            users.add(String.format("{ \"id\": \"u%d\", \"userName\": \"alice-%d\" }", i, i));
        }
        return users;
    }

    private static String usersBody(List<String> users, int totalResults) {
        return "{ \"totalResults\": " + totalResults + ", \"Resources\": [" + String.join(",", users) + "] }";
    }

    /**
     * Filter {@link #requestedQueries} to only the queries whose path was {@code /Users}.
     * /Groups fetches go through the same handler (and use the same paginated walk),
     * so the raw queries list is interleaved.
     */
    private List<String> userQueries() {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < requestedPaths.size(); i++) {
            if ("/Users".equals(requestedPaths.get(i))) out.add(requestedQueries.get(i));
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────
    // entityIdFor — pure helper
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void entityIdFor_formatsAsSubKindDotConnectorDotSlug() {
        assertThat(ScimSyncEngine.entityIdFor("okta", "user", "alice")).isEqualTo("user.okta.alice");
        assertThat(ScimSyncEngine.entityIdFor("okta", "group", "engineering")).isEqualTo("group.okta.engineering");
    }

    @Test
    void entityIdFor_lowercasesAndCollapsesNonAlnumIntoSingleDash() {
        assertThat(ScimSyncEngine.entityIdFor("okta", "user", "Alice O'Connor")).isEqualTo("user.okta.alice-o-connor");
        assertThat(ScimSyncEngine.entityIdFor("okta", "group", "  Team Alpha / Beta  ")).isEqualTo("group.okta.team-alpha-beta");
    }

    @Test
    void entityIdFor_trimsLeadingAndTrailingDashes() {
        assertThat(ScimSyncEngine.entityIdFor("okta", "user", "###bob###")).isEqualTo("user.okta.bob");
    }

    @Test
    void entityIdFor_keepsDotsAndUnderscoresVerbatim() {
        // Backend EntityIdValidator regex accepts [a-z0-9._-]; userName like
        // "alice.smith" or "alice_smith" must round-trip through slugify
        // without being mangled into dashes (the canonical layout is then
        // user.<connector>.alice.smith rather than user.<connector>.alice-smith).
        assertThat(ScimSyncEngine.entityIdFor("okta", "user", "alice.smith")).isEqualTo("user.okta.alice.smith");
        assertThat(ScimSyncEngine.entityIdFor("okta", "user", "alice_smith")).isEqualTo("user.okta.alice_smith");
        assertThat(ScimSyncEngine.entityIdFor("okta", "group", "team-alpha")).isEqualTo("group.okta.team-alpha");
    }

    @Test
    void entityIdFor_emptyRawIdYieldsEmptySlug() {
        // Documents current behaviour; caller is expected to filter blanks
        // out before calling. The contract is locked here so a future refactor
        // doesn't silently change uid layout.
        assertThat(ScimSyncEngine.entityIdFor("okta", "user", "")).isEqualTo("user.okta.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // sync() — over embedded HttpServer
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void sync_importsGroupsAndUsers_andUpsertsThroughEntityAdminApi() {
        responses.put(
            "/Groups",
            """
            { "totalResults": 1, "Resources": [
              { "id": "g1", "displayName": "engineering", "members": [{"value":"u1"}, {"value":"u2"}] }
            ]}
            """
        );
        responses.put(
            "/Users",
            """
            { "totalResults": 1, "Resources": [
              { "id": "u1", "userName": "alice", "displayName": "Alice",
                "active": true,
                "name": {"formatted": "Alice Anderson"},
                "emails": [{"value": "alice@acme.io"}],
                "groups": [{"display": "engineering"}] }
            ]}
            """
        );
        when(entityApi.find(eq("env-1"), any(EntityFilter.class))).thenReturn(List.of());
        when(entityApi.upsert(any(AuthzCallerContext.class), any(CreateOrReplaceEntityCommand.class)))
            .thenAnswer(inv -> upsertResult(inv.<CreateOrReplaceEntityCommand>getArgument(1).entityId(), true));

        ScimSyncEngine.SyncResult result = engine.sync(connector(true, true));

        assertThat(result.error).isNull();
        assertThat(result.warnings).isEmpty();
        assertThat(result.users).isEqualTo(1);
        assertThat(result.groups).isEqualTo(1);
        assertThat(result.deleted).isZero();
        // UpsertResult.created=true on every call → both records are new
        assertThat(result.usersCreated).isEqualTo(1);
        assertThat(result.groupsCreated).isEqualTo(1);

        // Group upsert: kind=PRINCIPAL, source=scim, no parents.
        // name is the raw SCIM displayName ("engineering"). No separate
        // "displayName" attribute is written — name is the single source
        // of truth for the group label.
        verify(entityApi)
            .upsert(
                argThat(AuthzCallerContext::isSystem),
                argThat(cmd ->
                    cmd.entityId().equals("group.okta.engineering") &&
                    cmd.kind() == EntityKind.PRINCIPAL &&
                    cmd.source().equals("scim") &&
                    "group".equals(cmd.attributes().get("_kind")) &&
                    "okta".equals(cmd.attributes().get("_connector")) &&
                    "engineering".equals(cmd.attributes().get("name")) &&
                    !cmd.attributes().containsKey("displayName") &&
                    Integer.valueOf(2).equals(cmd.attributes().get("memberCount")) &&
                    cmd.parents().isEmpty()
                )
            );
        // User upsert: name is the raw SCIM userName ("alice"). The
        // structured `name` object and the SCIM `userName` itself are
        // reserved (we don't pass them through), but every other scalar
        // attribute on the payload — including SCIM `displayName` — is
        // copied verbatim by copyScalarAttributes.
        verify(entityApi)
            .upsert(
                argThat(AuthzCallerContext::isSystem),
                argThat(cmd ->
                    cmd.entityId().equals("user.okta.alice") &&
                    cmd.kind() == EntityKind.PRINCIPAL &&
                    "user".equals(cmd.attributes().get("_kind")) &&
                    "alice@acme.io".equals(cmd.attributes().get("email")) &&
                    "alice".equals(cmd.attributes().get("name")) &&
                    "Alice".equals(cmd.attributes().get("displayName")) &&
                    !cmd.attributes().containsKey("userName") &&
                    Boolean.TRUE.equals(cmd.attributes().get("active")) &&
                    cmd.parents().equals(List.of("group.okta.engineering"))
                )
            );
    }

    @Test
    void sync_passesThroughScalarUserAttributes_verbatim() {
        // Anything SCIM-side that's a scalar (text/number/boolean) and not
        // explicitly handled by the engine should land on the entity as-is
        // so the UI surfaces whatever the upstream IdP chose to publish
        // (title, userType, preferredLanguage, locale, externalId, …).
        responses.put("/Groups", "{ \"Resources\": [] }");
        responses.put(
            "/Users",
            """
            { "Resources": [
              { "id": "u1",
                "userName": "alice",
                "title": "Staff Engineer",
                "userType": "Employee",
                "preferredLanguage": "en-US",
                "locale": "en-US",
                "externalId": "alice@corp.local",
                "active": true,
                "loginCount": 42 }
            ]}
            """
        );
        when(entityApi.find(eq("env-1"), any(EntityFilter.class))).thenReturn(List.of());
        when(entityApi.upsert(any(AuthzCallerContext.class), any(CreateOrReplaceEntityCommand.class)))
            .thenAnswer(inv -> upsertResult(inv.<CreateOrReplaceEntityCommand>getArgument(1).entityId(), true));

        engine.sync(connector(true, true));

        verify(entityApi)
            .upsert(
                any(AuthzCallerContext.class),
                argThat(cmd ->
                    "Staff Engineer".equals(cmd.attributes().get("title")) &&
                    "Employee".equals(cmd.attributes().get("userType")) &&
                    "en-US".equals(cmd.attributes().get("preferredLanguage")) &&
                    "en-US".equals(cmd.attributes().get("locale")) &&
                    "alice@corp.local".equals(cmd.attributes().get("externalId")) &&
                    Integer.valueOf(42).equals(cmd.attributes().get("loginCount"))
                )
            );
    }

    @Test
    void sync_skipsNestedObjectsAndExtensionUris_whenPassingThroughAttributes() {
        // Nested objects (SCIM `name`, addresses, enterprise extension) and
        // extension URIs (urn:…) are skipped because:
        //   1. the entity-detail UI only renders scalars,
        //   2. Mongo rejects '.' and ':' in field names, so storing them
        //      as-is would blow up on persist.
        responses.put("/Groups", "{ \"Resources\": [] }");
        responses.put(
            "/Users",
            """
            { "Resources": [
              { "id": "u1",
                "userName": "alice",
                "title": "Staff Engineer",
                "name": {"formatted": "Alice Anderson", "givenName": "Alice"},
                "addresses": [{"streetAddress": "1 Main St"}],
                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
                  "department": "Engineering",
                  "manager": {"value": "u9"}
                } }
            ]}
            """
        );
        when(entityApi.find(eq("env-1"), any(EntityFilter.class))).thenReturn(List.of());
        when(entityApi.upsert(any(AuthzCallerContext.class), any(CreateOrReplaceEntityCommand.class)))
            .thenAnswer(inv -> upsertResult(inv.<CreateOrReplaceEntityCommand>getArgument(1).entityId(), true));

        engine.sync(connector(true, true));

        verify(entityApi)
            .upsert(
                any(AuthzCallerContext.class),
                argThat(cmd ->
                    "Staff Engineer".equals(cmd.attributes().get("title")) &&
                    !cmd.attributes().containsKey("name.formatted") &&
                    !cmd.attributes().containsKey("addresses") &&
                    cmd.attributes().keySet().stream().noneMatch(k -> k.startsWith("urn:")) &&
                    cmd.attributes().keySet().stream().noneMatch(k -> k.contains("."))
                )
            );
    }

    @Test
    void sync_passesThroughScalarGroupAttributes_verbatim() {
        // Same pass-through for groups — anything beyond id/displayName/members
        // lands on the entity as-is (description, externalId, …).
        responses.put(
            "/Groups",
            """
            { "Resources": [
              { "id": "g1",
                "displayName": "engineering",
                "description": "Core engineering team",
                "externalId": "okta-eng-001",
                "members": [{"value":"u1"}] }
            ]}
            """
        );
        responses.put("/Users", "{ \"Resources\": [] }");
        when(entityApi.find(eq("env-1"), any(EntityFilter.class))).thenReturn(List.of());
        when(entityApi.upsert(any(AuthzCallerContext.class), any(CreateOrReplaceEntityCommand.class)))
            .thenAnswer(inv -> upsertResult(inv.<CreateOrReplaceEntityCommand>getArgument(1).entityId(), true));

        engine.sync(connector(true, true));

        verify(entityApi)
            .upsert(
                any(AuthzCallerContext.class),
                argThat(cmd ->
                    cmd.entityId().equals("group.okta.engineering") &&
                    "engineering".equals(cmd.attributes().get("name")) &&
                    "Core engineering team".equals(cmd.attributes().get("description")) &&
                    "okta-eng-001".equals(cmd.attributes().get("externalId")) &&
                    !cmd.attributes().containsKey("displayName") &&
                    !cmd.attributes().containsKey("members") &&
                    Integer.valueOf(1).equals(cmd.attributes().get("memberCount"))
                )
            );
    }

    @Test
    void sync_skipsGroupsFetch_whenImportGroupsFalse() {
        responses.put("/Users", "{ \"Resources\": [] }");
        when(entityApi.find(eq("env-1"), any(EntityFilter.class))).thenReturn(List.of());

        ScimSyncEngine.SyncResult result = engine.sync(connector(true, false));

        assertThat(requestedPaths).noneMatch(p -> p.endsWith("/Groups"));
        assertThat(requestedPaths).anyMatch(p -> p.endsWith("/Users"));
        assertThat(result.error).isNull();
        assertThat(result.groups).isZero();
    }

    @Test
    void sync_skipsUsersFetch_whenImportUsersFalse() {
        responses.put("/Groups", "{ \"Resources\": [] }");
        when(entityApi.find(eq("env-1"), any(EntityFilter.class))).thenReturn(List.of());

        ScimSyncEngine.SyncResult result = engine.sync(connector(false, true));

        assertThat(requestedPaths).noneMatch(p -> p.endsWith("/Users"));
        assertThat(requestedPaths).anyMatch(p -> p.endsWith("/Groups"));
        assertThat(result.error).isNull();
        assertThat(result.users).isZero();
    }

    @Test
    void sync_countsUpdatesSeparatelyFromCreates() {
        // UpsertResult.created=false → the engine treats the call as an update
        // and bumps users/groups but NOT usersCreated/groupsCreated. This lets
        // a noisy sync (only attribute updates) be distinguished in the
        // dashboard from an inrush of new principals.
        responses.put("/Groups", "{ \"Resources\": [ { \"id\": \"g1\", \"displayName\": \"engineering\" } ]}");
        responses.put("/Users", "{ \"Resources\": [ { \"id\": \"u1\", \"userName\": \"alice\" } ]}");
        when(entityApi.find(eq("env-1"), any(EntityFilter.class))).thenReturn(List.of());
        when(entityApi.upsert(any(AuthzCallerContext.class), any(CreateOrReplaceEntityCommand.class)))
            .thenAnswer(inv -> upsertResult(inv.<CreateOrReplaceEntityCommand>getArgument(1).entityId(), false));

        ScimSyncEngine.SyncResult result = engine.sync(connector(true, true));

        assertThat(result.users).isEqualTo(1);
        assertThat(result.groups).isEqualTo(1);
        assertThat(result.usersCreated).isZero();
        assertThat(result.groupsCreated).isZero();
    }

    @Test
    void sync_skipsResourcesWithBlankIdentifier() {
        // Engine drops Groups whose displayName is blank and Users whose userName
        // is blank before calling upsert — silent skip, no warning.
        responses.put(
            "/Groups",
            """
            { "Resources": [
              { "id": "g1", "displayName": "" },
              { "id": "g2", "displayName": "  " },
              { "id": "g3", "displayName": "engineering" }
            ]}
            """
        );
        responses.put(
            "/Users",
            """
            { "Resources": [
              { "id": "u1", "userName": "" },
              { "id": "u2", "userName": "alice" }
            ]}
            """
        );
        when(entityApi.find(eq("env-1"), any(EntityFilter.class))).thenReturn(List.of());
        when(entityApi.upsert(any(AuthzCallerContext.class), any(CreateOrReplaceEntityCommand.class)))
            .thenAnswer(inv -> upsertResult(inv.<CreateOrReplaceEntityCommand>getArgument(1).entityId(), true));

        ScimSyncEngine.SyncResult result = engine.sync(connector(true, true));

        assertThat(result.groups).isEqualTo(1);
        assertThat(result.users).isEqualTo(1);
        verify(entityApi, times(2)).upsert(any(AuthzCallerContext.class), any(CreateOrReplaceEntityCommand.class));
    }

    @Test
    void sync_walksAllPages_whenTotalResultsExceedsPageSize() {
        // Server has 250 users across 3 pages (100 + 100 + 50). The engine must
        // walk all of them and not stop at the first page — that's the data-loss
        // fix from review #6: stopping at page 1 caused reconcileDeletes to wipe
        // pages 2+ as orphans on the next sync.
        responses.put("/Groups", "{ \"totalResults\": 0, \"Resources\": [] }");
        dynamicResponses.put("/Users", uri -> usersPage(uri, /* total */ 250, /* pageSize */ 100));
        when(entityApi.find(eq("env-1"), any(EntityFilter.class))).thenReturn(List.of());
        when(entityApi.upsert(any(AuthzCallerContext.class), any(CreateOrReplaceEntityCommand.class)))
            .thenAnswer(inv -> upsertResult(inv.<CreateOrReplaceEntityCommand>getArgument(1).entityId(), true));

        ScimSyncEngine.SyncResult result = engine.sync(connector(true, true));

        assertThat(result.error).isNull();
        assertThat(result.warnings).isEmpty();
        assertThat(result.users).isEqualTo(250);
        // 3 paginated calls: startIndex=1, 101, 201 — last one returns the short tail (50)
        // and terminates the walk.
        assertThat(userQueries()).containsExactly("startIndex=1&count=100", "startIndex=101&count=100", "startIndex=201&count=100");
    }

    @Test
    void sync_stopsWalking_whenServerReturnsShortPage() {
        // When the upstream returns fewer than PAGE_SIZE records, the engine
        // treats it as the last page and stops — even if totalResults disagrees
        // (some IdPs are sloppy about totalResults under filtering).
        responses.put("/Groups", "{ \"Resources\": [] }");
        dynamicResponses.put(
            "/Users",
            uri -> {
                int startIndex = parseStartIndex(uri);
                if (startIndex == 1) {
                    // 50 records — fewer than count=100, so last page.
                    return usersBody(buildUsers(1, 50), /* totalResults */ 9999);
                }
                throw new AssertionError("should not request a second page; startIndex=" + startIndex);
            }
        );
        when(entityApi.find(eq("env-1"), any(EntityFilter.class))).thenReturn(List.of());
        when(entityApi.upsert(any(AuthzCallerContext.class), any(CreateOrReplaceEntityCommand.class)))
            .thenAnswer(inv -> upsertResult(inv.<CreateOrReplaceEntityCommand>getArgument(1).entityId(), true));

        ScimSyncEngine.SyncResult result = engine.sync(connector(true, true));

        assertThat(result.users).isEqualTo(50);
        assertThat(userQueries()).containsExactly("startIndex=1&count=100");
    }

    @Test
    void sync_stopsWalking_whenTotalResultsReached() {
        // Server has exactly 200 users in two full pages; the engine should
        // request page 1 + page 2, then stop because cumulative seen reaches
        // totalResults — without firing a third (empty) request.
        responses.put("/Groups", "{ \"Resources\": [] }");
        dynamicResponses.put("/Users", uri -> usersPage(uri, /* total */ 200, /* pageSize */ 100));
        when(entityApi.find(eq("env-1"), any(EntityFilter.class))).thenReturn(List.of());
        when(entityApi.upsert(any(AuthzCallerContext.class), any(CreateOrReplaceEntityCommand.class)))
            .thenAnswer(inv -> upsertResult(inv.<CreateOrReplaceEntityCommand>getArgument(1).entityId(), true));

        ScimSyncEngine.SyncResult result = engine.sync(connector(true, true));

        assertThat(result.users).isEqualTo(200);
        assertThat(userQueries()).containsExactly("startIndex=1&count=100", "startIndex=101&count=100");
    }

    @Test
    void sync_doesNotSweepPage2OrphansAfterPaginationWasAdded() {
        // Regression for review #6 — before pagination, the engine fetched
        // only page 1 (100 users), expectedEntityIds was 100 users, and any
        // SCIM-sourced entity from pages 2+ already in the env was treated
        // as an orphan and deleted on every sync. With pagination, page 2+
        // entities are now in expectedEntityIds and reconcileDeletes leaves
        // them alone.
        responses.put("/Groups", "{ \"Resources\": [] }");
        dynamicResponses.put("/Users", uri -> usersPage(uri, /* total */ 150, /* pageSize */ 100));
        // user.okta.alice-101 lives on page 2 of the upstream. It already
        // exists in the env. Without pagination, the engine would have
        // deleted it; with pagination it must NOT.
        Entity page2Mirror = scimEntity("user.okta.alice-101", "okta");
        when(entityApi.find(eq("env-1"), any(EntityFilter.class))).thenReturn(List.of(page2Mirror));
        when(entityApi.upsert(any(AuthzCallerContext.class), any(CreateOrReplaceEntityCommand.class)))
            .thenAnswer(inv -> upsertResult(inv.<CreateOrReplaceEntityCommand>getArgument(1).entityId(), false));

        ScimSyncEngine.SyncResult result = engine.sync(connector(true, true));

        assertThat(result.users).isEqualTo(150);
        assertThat(result.deleted).isZero();
        verify(entityApi, never()).delete(any(AuthzCallerContext.class), eq("user.okta.alice-101"));
    }

    @Test
    void sync_capturesError_whenGroupsEndpointFails() {
        server.removeContext("/Groups");
        // /Groups now 404s. /Users handler still works but the engine short-circuits
        // because error was set in the groups branch — entityApi.find/delete are
        // never called and we deliberately omit their stubs (strict-mode mocking).

        ScimSyncEngine.SyncResult result = engine.sync(connector(true, true));

        assertThat(result.error).contains("/Groups");
        assertThat(result.users).isZero();
        assertThat(requestedPaths).noneMatch(p -> p.endsWith("/Users"));
        verify(entityApi, never()).find(any(), any());
        verify(entityApi, never()).delete(any(AuthzCallerContext.class), any());
    }

    @Test
    void sync_reconcilesDeletes_onlyOrphansOwnedByThisConnector() {
        responses.put("/Groups", "{ \"Resources\": [] }");
        responses.put(
            "/Users",
            """
            { "Resources": [
              { "id": "u1", "userName": "alice" }
            ]}
            """
        );
        // Three SCIM-sourced entities in the env: alice is still in upstream, bob
        // is owned by us but no longer present (orphan), charlie@azure is owned
        // by a different connector and must not be touched.
        Entity alice = scimEntity("user.okta.alice", "okta");
        Entity bob = scimEntity("user.okta.bob", "okta");
        Entity charlie = scimEntity("user.azure.charlie", "azure");
        when(entityApi.find(eq("env-1"), any(EntityFilter.class))).thenReturn(List.of(alice, bob, charlie));
        when(entityApi.upsert(any(AuthzCallerContext.class), any(CreateOrReplaceEntityCommand.class)))
            .thenAnswer(inv -> upsertResult(inv.<CreateOrReplaceEntityCommand>getArgument(1).entityId(), false));

        ScimSyncEngine.SyncResult result = engine.sync(connector(true, true));

        assertThat(result.deleted).isEqualTo(1);
        verify(entityApi).delete(any(AuthzCallerContext.class), eq("user.okta.bob"));
        verify(entityApi, never()).delete(any(AuthzCallerContext.class), eq("user.okta.alice"));
        verify(entityApi, never()).delete(any(AuthzCallerContext.class), eq("user.azure.charlie"));
    }
}
