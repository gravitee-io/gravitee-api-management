package io.gravitee.gamma.module.authz.entityimport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.api.EntityAdminApi;
import io.gravitee.gamma.authorization.domain.Entity;
import io.gravitee.gamma.authorization.domain.EntityKind;
import io.gravitee.gamma.authorization.service.CreateOrReplaceEntityCommand;
import io.gravitee.gamma.authorization.service.EntityFilter;
import io.gravitee.gamma.authorization.service.UpsertResult;
import io.gravitee.gamma.module.authz.entityimport.repository.ScimConnectorDocument;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Pulls users/groups from a SCIM 2.0 endpoint and reconciles them with the local
 * entity graph for a given {@link ScimConnectorDocument}: upserts what the source
 * still has, deletes what disappeared.
 *
 * <p>Each SCIM-sourced entity:
 * <ul>
 *   <li>is kind {@link EntityKind#PRINCIPAL} (sub-kind {@code user}/{@code group} stored as attribute)</li>
 *   <li>carries {@code source = "scim"} on the platform record (used by reconcile to scope the sweep)</li>
 *   <li>carries {@code _connector = <connectorName>} attribute so multiple SCIM connectors in the
 *       same environment do not delete each other's entities</li>
 *   <li>has {@code entityId} layout {@code <subKind>.<connector>.<slug>} (e.g. {@code user.okta.alice})</li>
 * </ul>
 *
 * <p>All writes go through {@link EntityAdminApi} so that gateway sync, audit, and validation
 * see SCIM-driven changes exactly like any user-driven change.
 */
@Service
public class ScimSyncEngine {

    static final String SOURCE_SCIM = "scim";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * SCIM user attributes that the engine writes itself (via {@link #upsertUser}).
     * Everything else on a SCIM user payload is copied through to entity attributes
     * verbatim by {@link #copyScalarAttributes} so the entity surfaces whatever the
     * upstream IdP chose to publish ({@code title}, {@code userType}, {@code preferredLanguage}, …).
     */
    private static final Set<String> USER_RESERVED_KEYS = Set.of("id", "schemas", "meta", "userName", "name", "groups", "emails");

    private static final Set<String> GROUP_RESERVED_KEYS = Set.of("id", "schemas", "meta", "displayName", "members");

    /**
     * SCIM 2.0 page size for paginated reads. The upstream is queried with
     * {@code ?startIndex=N&count=PAGE_SIZE} and the engine walks pages until
     * either the server says it's done ({@code totalResults} reached or a
     * short page is returned) or {@link #MAX_PAGES} pages have been fetched.
     */
    private static final int PAGE_SIZE = 100;

    /**
     * Hard ceiling on pages fetched per resource type, per sync run.
     * Prevents a buggy SCIM endpoint from looping forever (e.g. one that
     * keeps returning the same page or never updates {@code startIndex}).
     * At PAGE_SIZE=100 this caps a single sync at ~100k principals — more
     * than enough for any sensible tenant. If you hit this, increase
     * deliberately and add a sync-time budget alongside.
     */
    private static final int MAX_PAGES = 1000;

    private final EntityAdminApi entityApi;
    private final HttpClient http;

    @Autowired
    public ScimSyncEngine(EntityAdminApi entityApi) {
        this.entityApi = entityApi;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public static final class SyncResult {

        public int users;
        public int groups;
        public int deleted;

        /**
         * Subset of {@link #users}/{@link #groups} that were newly created
         * by this sync (the rest were updates to existing entities). Surfaced
         * separately so callers can show e.g. "synced 5 users (3 new)" in
         * the dashboard and so a noisy sync (only updates) is distinguishable
         * from an inrush of new principals.
         */
        public int usersCreated;
        public int groupsCreated;

        public final List<String> warnings = new ArrayList<>();
        public String error;
    }

    public SyncResult sync(ScimConnectorDocument c, String token) {
        SyncResult r = new SyncResult();
        String base = stripTrailingSlash(c.getUrl());
        Set<String> expectedEntityIds = new HashSet<>();

        if (c.isImportGroups()) {
            try {
                walkScimPages(base + "/Groups", token, "/Groups", r, group -> {
                    String display = safeText(group, "displayName");
                    if (display == null || display.isBlank()) return;
                    String entityId = entityIdFor(c.getName(), "group", display);
                    try {
                        UpsertResult outcome = upsertGroup(c, entityId, display, group);
                        expectedEntityIds.add(entityId);
                        r.groups++;
                        if (outcome.created()) r.groupsCreated++;
                    } catch (Exception ex) {
                        r.warnings.add("group " + display + ": " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                r.error = "Failed to fetch /Groups: " + ex.getMessage();
            }
        }

        if (c.isImportUsers() && r.error == null) {
            try {
                walkScimPages(base + "/Users", token, "/Users", r, user -> {
                    String userName = safeText(user, "userName");
                    if (userName == null || userName.isBlank()) return;
                    String entityId = entityIdFor(c.getName(), "user", userName);
                    try {
                        UpsertResult outcome = upsertUser(c, entityId, userName, user);
                        expectedEntityIds.add(entityId);
                        r.users++;
                        if (outcome.created()) r.usersCreated++;
                    } catch (Exception ex) {
                        r.warnings.add("user " + userName + ": " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                r.error = "Failed to fetch /Users: " + ex.getMessage();
            }
        }

        if (r.error == null) {
            // reconcileDeletes is now safe because expectedEntityIds covers every
            // page the upstream served, not just the first 100 — fixing the
            // data-loss-by-orphan-sweep risk flagged in code review #6.
            r.deleted = reconcileDeletes(c, expectedEntityIds);
        }
        return r;
    }

    /**
     * Walk a SCIM 2.0 paginated resource collection.
     *
     * <p>Pages are requested with {@code ?startIndex=N&count=PAGE_SIZE} (1-based
     * per RFC 7644 §3.4.2). Termination conditions:
     * <ul>
     *   <li>page returns 0 records (defensive: pagination semantics ill-defined),</li>
     *   <li>cumulative records reach the server's reported {@code totalResults},</li>
     *   <li>fewer than {@link #PAGE_SIZE} records returned (last page),</li>
     *   <li>{@link #MAX_PAGES} pages fetched (capped to prevent infinite loops
     *       on broken upstreams).</li>
     * </ul>
     *
     * <p>A warning is emitted when MAX_PAGES is hit because the sync became
     * partial — reconcile-delete will then skip orphans it can't see, but
     * the dashboard surface needs to reflect that the upstream wasn't fully
     * drained.
     */
    private void walkScimPages(String baseUrl, String token, String pathLabel, SyncResult r, java.util.function.Consumer<JsonNode> onEach)
        throws Exception {
        int startIndex = 1;
        int seen = 0;
        Integer totalResults = null;
        for (int page = 0; page < MAX_PAGES; page++) {
            String url = baseUrl + "?startIndex=" + startIndex + "&count=" + PAGE_SIZE;
            JsonNode resp = fetchScim(url, token);
            JsonNode resources = arrayOrEmpty(resp.get("Resources"));
            int pageSize = resources.size();
            if (pageSize == 0) return;
            for (JsonNode res : resources) {
                onEach.accept(res);
            }
            seen += pageSize;
            if (totalResults == null) {
                JsonNode total = resp.get("totalResults");
                if (total != null && total.canConvertToInt()) totalResults = total.asInt();
            }
            if (totalResults != null && seen >= totalResults) return;
            if (pageSize < PAGE_SIZE) return;
            startIndex += pageSize;
        }
        r.warnings.add(
            pathLabel +
                ": stopped after " +
                MAX_PAGES +
                " pages (~" +
                (MAX_PAGES * PAGE_SIZE) +
                " records) — upstream may be paginating incorrectly"
        );
    }

    /**
     * Sweep platform entities whose {@code source = "scim"} and whose {@code _connector}
     * attribute matches this connector's name, deleting any whose {@code entityId} is no
     * longer present in {@code expectedEntityIds}.
     */
    private int reconcileDeletes(ScimConnectorDocument c, Set<String> expectedEntityIds) {
        int removed = 0;
        AuthzCallerContext caller = AuthzCallerContext.system(c.getEnvironmentId());
        List<Entity> existing = entityApi.find(c.getEnvironmentId(), new EntityFilter(null, SOURCE_SCIM, null));
        for (Entity e : existing) {
            Object owner = e.attributes().get("_connector");
            if (owner == null || !c.getName().equals(owner.toString())) continue;
            if (expectedEntityIds.contains(e.entityId())) continue;
            try {
                entityApi.delete(caller, e.entityId());
                removed++;
            } catch (Exception ignored) {
                // best-effort sweep
            }
        }
        return removed;
    }

    /**
     * Build a SCIM-sourced principal entity.
     *
     * <p>{@link #copyScalarAttributes} first does a verbatim pass-through of
     * every scalar field on the SCIM payload (title, userType, preferredLanguage,
     * externalId, …) so the entity surfaces whatever the upstream IdP chose to
     * publish. The engine then overrides a small set of canonical fields:
     * <ul>
     *   <li>{@code name} — the raw SCIM {@code userName} (the UI's display label)</li>
     *   <li>{@code scimId} — the SCIM {@code id} (IdP correlation)</li>
     *   <li>{@code active} — defaults to {@code true} when absent</li>
     *   <li>{@code email} — extracted from the first entry of the {@code emails} array</li>
     * </ul>
     * {@code userName} itself is reserved (it'd duplicate {@code name}), the
     * structured {@code name} object is reserved (Mongo rejects dotted keys),
     * and the {@code groups} array is reserved (consumed below to populate parents).
     */
    private UpsertResult upsertUser(ScimConnectorDocument c, String entityId, String userName, JsonNode u) {
        Map<String, Object> attrs = new HashMap<>();
        baseAttrs(attrs, c, "user");
        copyScalarAttributes(u, attrs, USER_RESERVED_KEYS);
        attrs.put("scimId", safeText(u, "id"));
        attrs.put("name", userName);
        attrs.put("active", u.path("active").asBoolean(true));
        JsonNode emails = u.get("emails");
        if (emails != null && emails.isArray() && !emails.isEmpty()) {
            String email = safeText(emails.get(0), "value");
            if (email != null) attrs.put("email", email);
        }
        List<String> parents = new ArrayList<>();
        JsonNode groups = u.get("groups");
        if (groups != null && groups.isArray()) {
            for (JsonNode g : groups) {
                String gd = safeText(g, "display");
                if (gd != null && !gd.isBlank()) {
                    parents.add(entityIdFor(c.getName(), "group", gd));
                }
            }
        }
        return upsert(c.getEnvironmentId(), entityId, attrs, parents);
    }

    /**
     * Build a SCIM-sourced group entity.
     *
     * <p>{@link #copyScalarAttributes} first does a verbatim pass-through of
     * every scalar group field (description, externalId, …) so the entity
     * surfaces whatever the upstream IdP chose to publish. The engine then
     * overrides:
     * <ul>
     *   <li>{@code name} — the raw SCIM {@code displayName} (used for membership lookup)</li>
     *   <li>{@code scimId} — the SCIM {@code id} (IdP correlation)</li>
     *   <li>{@code memberCount} — convenience scalar for the dashboard</li>
     * </ul>
     * {@code displayName} itself is reserved (it'd duplicate {@code name}) and
     * the {@code members} array is reserved (we only carry the count).
     */
    private UpsertResult upsertGroup(ScimConnectorDocument c, String entityId, String displayName, JsonNode g) {
        Map<String, Object> attrs = new HashMap<>();
        baseAttrs(attrs, c, "group");
        copyScalarAttributes(g, attrs, GROUP_RESERVED_KEYS);
        attrs.put("scimId", safeText(g, "id"));
        attrs.put("name", displayName);
        JsonNode members = g.get("members");
        attrs.put("memberCount", members != null && members.isArray() ? members.size() : 0);
        return upsert(c.getEnvironmentId(), entityId, attrs, List.of());
    }

    /**
     * Pass-through copy of scalar SCIM attributes (text/number/boolean) onto the
     * entity attributes map. Anything in {@code reservedKeys} is skipped (the engine
     * writes those explicitly), and the following are also skipped:
     * <ul>
     *   <li>extension schema URIs (keys starting with {@code urn:}) — these are
     *       structured maps keyed by URN and Mongo rejects {@code .}/{@code :}
     *       in field names;</li>
     *   <li>nested objects and arrays — the entity detail panel only renders
     *       flat scalars, and Mongo rejects {@code .} inside nested keys;</li>
     *   <li>keys containing {@code .} or {@code $} — invalid Mongo field names;</li>
     *   <li>{@code null} values — leave the attribute absent rather than null.</li>
     * </ul>
     */
    private static void copyScalarAttributes(JsonNode node, Map<String, Object> target, Set<String> reservedKeys) {
        if (node == null || !node.isObject()) return;
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String key = entry.getKey();
            if (key == null || reservedKeys.contains(key)) continue;
            if (key.startsWith("urn:") || key.contains(".") || key.contains("$")) continue;
            JsonNode v = entry.getValue();
            if (v == null || v.isNull() || v.isObject() || v.isArray()) continue;
            if (v.isBoolean()) target.put(key, v.asBoolean());
            else if (v.isInt() || v.isShort()) target.put(key, v.asInt());
            else if (v.isLong()) target.put(key, v.asLong());
            else if (v.isDouble() || v.isFloat() || v.isBigDecimal()) target.put(key, v.asDouble());
            else target.put(key, v.asText());
        }
    }

    private static void baseAttrs(Map<String, Object> attrs, ScimConnectorDocument c, String subKind) {
        attrs.put("_connector", c.getName());
        attrs.put("_kind", subKind);
        attrs.put("_url", c.getUrl());
        attrs.put("_syncedAt", Instant.now().toString());
    }

    private UpsertResult upsert(String environmentId, String entityId, Map<String, Object> attrs, List<String> parents) {
        return entityApi.upsert(
            AuthzCallerContext.system(environmentId),
            new CreateOrReplaceEntityCommand(environmentId, entityId, EntityKind.PRINCIPAL, attrs, parents, SOURCE_SCIM)
        );
    }

    private JsonNode fetchScim(String url, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/scim+json")
            .GET();
        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode());
        }
        return MAPPER.readTree(resp.body());
    }

    public static String entityIdFor(String connector, String subKind, String rawId) {
        return subKind + "." + connector + "." + slugify(rawId);
    }

    /**
     * Lowercase + replace any character not in the backend's {@code EntityIdValidator}
     * regex ({@code [a-z0-9._-]}) with a dash, then collapse leading/trailing dashes.
     *
     * <p>Dots, underscores and dashes pass through verbatim: a SCIM
     * {@code userName} of {@code alice.smith} stays {@code alice.smith}
     * (not {@code alice-smith}) and {@code alice_smith} stays
     * {@code alice_smith}. Anything else (whitespace, slashes, apostrophes
     * etc) still collapses to single dashes so the resulting entityId is
     * always {@code EntityIdValidator}-clean.
     */
    private static String slugify(String s) {
        if (s == null) return "";
        String normalised = s.toLowerCase().replaceAll("[^a-z0-9._-]+", "-");
        return normalised.replaceAll("^-+|-+$", "");
    }

    private static JsonNode arrayOrEmpty(JsonNode n) {
        return (n != null && n.isArray()) ? n : MAPPER.createArrayNode();
    }

    private static String safeText(JsonNode n, String f) {
        if (n == null) return null;
        JsonNode v = n.get(f);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
