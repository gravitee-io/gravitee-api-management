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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.eventbus.Message;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

@CustomLog
public class EventBusAuthzEnginePort implements AuthzEnginePort {

    public static final String SCOPE_ADDRESS_PREFIX = "service:authz-pdp:sync:scope:";
    public static final String DEFAULT_ADDRESS = "service:authz-pdp:sync";
    static final String WILDCARD = "*";
    static final String DEFAULT_SCOPE = "default";
    // A routing scope is "<targetPdpId>@<tag>" when tagged (e.g. "default@us", "orders@eu"), else the bare
    // targetPdpId. The tag selects WHERE the engine runs (enforced by AuthzHostedScopes#serves), not the
    // engine address: the address is derived from the targetPdpId part only, so "orders@us" and "orders@eu"
    // alias to the same per-scope engine address yet are hosted on different nodes.
    static final String SCOPE_TAG_SEPARATOR = "@";

    static final String OP_ADD_OR_UPDATE_ENTITY = "addOrUpdateEntity";
    static final String OP_REMOVE_ENTITY = "removeEntity";
    static final String OP_ADD_OR_UPDATE_POLICY = "addOrUpdatePolicy";
    static final String OP_REMOVE_POLICY = "removePolicy";
    static final String OP_COMMIT = "commit";

    // Vertx default reply timeout is 30s — long enough to wedge a sync cycle when the PDP service
    // is unhealthy. 5s is short enough to fail fast and long enough for normal in-process replies.
    static final long REPLY_TIMEOUT_MS = 5_000L;
    // Cap commit re-arming so a permanently dead scope stops paying a full reply timeout every cycle.
    static final int MAX_COMMIT_ATTEMPTS = 10;

    private final Vertx vertx;
    private final AuthzHostedScopes hostedScopes;
    private final DeliveryOptions deliveryOptions = new DeliveryOptions().setSendTimeout(REPLY_TIMEOUT_MS);
    private final Set<String> touched = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> commitAttempts = new ConcurrentHashMap<>();

    public EventBusAuthzEnginePort(Vertx vertx, AuthzHostedScopes hostedScopes) {
        this.vertx = Objects.requireNonNull(vertx, "vertx must not be null");
        this.hostedScopes = Objects.requireNonNull(hostedScopes, "hostedScopes must not be null");
    }

    private static String addressFor(String environmentId, String scope) {
        // Derive the engine address from the targetPdpId part only (strip "@tag"): a tagged scope aliases to
        // its base engine and tag scoping is enforced by AuthzHostedScopes#serves, not by a distinct address.
        // ("*" is expanded to concrete scopes before reaching here — see route()/expandWildcard.)
        String base = baseScope(scope);
        if (DEFAULT_SCOPE.equals(base)) {
            return DEFAULT_ADDRESS;
        }
        return SCOPE_ADDRESS_PREFIX + environmentId + ":" + base;
    }

    static String baseScope(String scope) {
        int at = scope.indexOf(SCOPE_TAG_SEPARATOR);
        return at >= 0 ? scope.substring(0, at) : scope;
    }

    @Override
    public Completable addOrUpdateEntity(
        String environmentId,
        String uid,
        Map<String, Object> attributes,
        List<String> parents,
        Set<String> targetPdpIds
    ) {
        JsonObject command = new JsonObject().put("op", OP_ADD_OR_UPDATE_ENTITY).put("uid", uid);
        if (attributes != null && !attributes.isEmpty()) {
            command.put("attributes", new JsonObject(attributes));
        }
        if (parents != null && !parents.isEmpty()) {
            command.put("parents", new JsonArray(parents));
        }
        return route(environmentId, command, targetPdpIds);
    }

    @Override
    public Completable removeEntity(String environmentId, String uid, Set<String> targetPdpIds) {
        return route(environmentId, new JsonObject().put("op", OP_REMOVE_ENTITY).put("uid", uid), targetPdpIds);
    }

    @Override
    public Completable addOrUpdatePolicy(String environmentId, String docId, String name, String policyText, Set<String> targetPdpIds) {
        JsonObject command = new JsonObject()
            .put("op", OP_ADD_OR_UPDATE_POLICY)
            .put("docId", docId)
            .put("name", name)
            .put("policyText", policyText);
        return route(environmentId, command, targetPdpIds);
    }

    @Override
    public Completable removePolicy(String environmentId, String docId, Set<String> targetPdpIds) {
        return route(environmentId, new JsonObject().put("op", OP_REMOVE_POLICY).put("docId", docId), targetPdpIds);
    }

    @Override
    public Completable commit() {
        return Completable.defer(this::commitTouchedSnapshot);
    }

    private Completable commitTouchedSnapshot() {
        List<String> addresses = new ArrayList<>(touched);
        // Remove only the snapshot we are committing; an address armed by another thread between the
        // copy and here survives for the next cycle instead of being silently dropped (clear() race).
        touched.removeAll(addresses);
        if (addresses.isEmpty()) {
            return Completable.complete();
        }
        List<Completable> commits = new ArrayList<>(addresses.size());
        for (String address : addresses) {
            commits.add(commitOne(address));
        }
        return Completable.merge(commits);
    }

    @Override
    public Completable commitScope(String environmentId, String targetPdpId) {
        return Completable.defer(() -> {
            String address = addressFor(environmentId, targetPdpId);
            // Commit only this scope's address. Draining the shared touched set here would couple this
            // scope's commit latency and failures to every other armed scope sharing this port, so a
            // single dead scope could stall every provision's hydration.
            touched.remove(address);
            return commitOne(address);
        });
    }

    private Completable commitOne(String address) {
        JsonObject c = new JsonObject().put("op", OP_COMMIT);
        return vertx
            .eventBus()
            .<JsonObject>rxRequest(address, c, deliveryOptions)
            .flatMapCompletable(this::verifyCommitReply)
            .doOnComplete(() -> commitAttempts.remove(address))
            .onErrorComplete(t -> {
                int attempts = commitAttempts.merge(address, 1, Integer::sum);
                if (attempts > MAX_COMMIT_ATTEMPTS) {
                    commitAttempts.remove(address);
                    log.warn("Abandoning authz PDP commit to address [{}] after {} attempts", address, attempts - 1);
                    return true;
                }
                // Re-arm the address so a transient commit failure is retried next cycle, instead of
                // leaving the scope's staged mutations unsealed until a later mutation touches it.
                touched.add(address);
                log.warn("Authz PDP commit to address [{}] failed (attempt {}), will retry next cycle", address, attempts, t);
                return true;
            });
    }

    private Completable route(String environmentId, JsonObject command, Set<String> targetPdpIds) {
        if (targetPdpIds == null || targetPdpIds.isEmpty()) {
            return Completable.complete();
        }
        List<Completable> sends = new ArrayList<>();
        for (String scope : expandWildcard(environmentId, targetPdpIds)) {
            // Only route to scopes this node hosts (default always served). A named scope whose engine
            // lives on another node is skipped — sending would just hit NO_HANDLERS.
            if (!hostedScopes.serves(environmentId, scope)) {
                continue;
            }
            String address = addressFor(environmentId, scope);
            sends.add(
                vertx
                    .eventBus()
                    .<JsonObject>rxRequest(address, command, deliveryOptions)
                    .ignoreElement()
                    .doOnComplete(() -> touched.add(address))
            );
        }
        return Completable.merge(sends);
    }

    // "*" means every engine in the environment. On a given node that is the set of scopes the node
    // actually hosts (it is the authority for its own placement, via AuthzHostedScopes) plus the always-on
    // default engine. Expanding to those and routing per-scope gives confirmed, retried delivery — unlike a
    // fire-and-forget broadcast — and a scope provisioned later is backfilled by the hydration path.
    private Set<String> expandWildcard(String environmentId, Set<String> targetPdpIds) {
        if (!targetPdpIds.contains(WILDCARD)) {
            return targetPdpIds;
        }
        Set<String> expanded = new LinkedHashSet<>(targetPdpIds);
        expanded.remove(WILDCARD);
        expanded.addAll(hostedScopes.hostedFor(environmentId));
        expanded.add(DEFAULT_SCOPE);
        return expanded;
    }

    private Completable verifyCommitReply(Message<JsonObject> reply) {
        JsonObject body = reply.body();
        Long gen = body != null ? body.getLong("commitGeneration") : null;
        if (gen == null) {
            return Completable.error(new IllegalStateException("Authz PDP commit reply missing 'commitGeneration': " + body));
        }
        log.debug("Authz engine commit landed, commitGeneration={}", gen);
        return Completable.complete();
    }
}
