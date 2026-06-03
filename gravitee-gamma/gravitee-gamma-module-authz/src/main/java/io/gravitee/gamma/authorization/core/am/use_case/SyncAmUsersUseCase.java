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
package io.gravitee.gamma.authorization.core.am.use_case;

import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.api.AuthzEntityAdminApi;
import io.gravitee.gamma.authorization.core.am.model.AmAgent;
import io.gravitee.gamma.authorization.core.am.model.AmGroup;
import io.gravitee.gamma.authorization.core.am.model.AmRole;
import io.gravitee.gamma.authorization.core.am.model.AmUser;
import io.gravitee.gamma.authorization.core.am.service_provider.AmDirectoryClient;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.service.CreateOrReplaceAuthzEntityCommand;
import io.gravitee.gamma.definition.authz.AgentEntityId;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.CustomLog;

/**
 * Pages every user, group, role, and agent out of the AM domain on the connection and upserts each
 * as a PRINCIPAL authz entity via {@link AuthzEntityAdminApi#bulkUpsert}. Groups and roles are synced
 * first (they are the parent entities users reference); each user's {@code parents} is then set to
 * its group + role ids. Agents (AM applications of type AGENT) are projected last as generic,
 * user-independent principals keyed on {@link AgentEntityId#derive(String, String)} so the request-time
 * PEP matches them. Upsert-only: entities removed in AM are left as stale entities. Invoked on a
 * worker thread by the infra job manager.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class SyncAmUsersUseCase {

    static final String SOURCE = "gravitee_am";

    private final AmDirectoryClient directoryClient;
    private final AuthzEntityAdminApi authzEntityAdminApi;
    private final SyncConfig syncConfig;

    public SyncAmUsersUseCase(AmDirectoryClient directoryClient, AuthzEntityAdminApi authzEntityAdminApi, SyncConfig syncConfig) {
        this.directoryClient = directoryClient;
        this.authzEntityAdminApi = authzEntityAdminApi;
        this.syncConfig = syncConfig;
    }

    public Output execute(Input input) {
        AuthzCallerContext caller = input.caller();
        String env = caller.environmentId();
        String domain = input.connection().defaultDomainId();

        int usersFetched = 0;
        int agentsFetched = 0;
        int entitiesUpserted = 0;
        List<CreateOrReplaceAuthzEntityCommand> batch = new ArrayList<>();

        try (AmDirectoryClient.Session session = directoryClient.openSession(input.connection())) {
            // Groups and roles first: they are the parent entities the user upserts reference.
            for (AmGroup group : pageThrough((p, s) -> {
                var gp = session.fetchGroups(p, s);
                return new Page<>(gp.groups(), gp.totalCount());
            }, syncConfig.pageSize())) {
                batch.add(groupCommand(env, group));
                if (batch.size() >= syncConfig.batchSize()) {
                    entitiesUpserted += flush(caller, batch);
                }
            }
            for (AmRole role : pageThrough((p, s) -> {
                var rp = session.fetchRoles(p, s);
                return new Page<>(rp.roles(), rp.totalCount());
            }, syncConfig.pageSize())) {
                batch.add(roleCommand(env, role));
                if (batch.size() >= syncConfig.batchSize()) {
                    entitiesUpserted += flush(caller, batch);
                }
            }
            // Flush the group/role remainder so the user phase starts on a clean batch.
            entitiesUpserted += flush(caller, batch);

            for (AmUser user : pageThrough((p, s) -> {
                var up = session.fetchUsers(p, s);
                return new Page<>(up.users(), up.totalCount());
            }, syncConfig.pageSize())) {
                usersFetched++;
                batch.add(userCommand(env, user));
                if (batch.size() >= syncConfig.batchSize()) {
                    entitiesUpserted += flush(caller, batch);
                }
                // Cap the sync at the configured ceiling; the lazy cursor stops paging once we break.
                if (usersFetched >= syncConfig.maxEntities()) {
                    break;
                }
            }
            // Flush the user remainder so the agent phase starts on a clean batch.
            entitiesUpserted += flush(caller, batch);

            for (AmAgent agent : pageThrough((p, s) -> {
                var ap = session.fetchAgents(p, s);
                return new Page<>(ap.agents(), ap.totalCount());
            }, syncConfig.pageSize())) {
                // The entity id is derived from the client_id; without one there is nothing the PEP
                // could match at request time, so skip the agent rather than upsert an unmatchable entity.
                if (agent.clientId() == null || agent.clientId().isBlank()) {
                    log.warn("Skipping AM agent {} in domain {}: it has no client_id", agent.id(), domain);
                    continue;
                }
                agentsFetched++;
                batch.add(agentCommand(env, domain, agent));
                if (batch.size() >= syncConfig.batchSize()) {
                    entitiesUpserted += flush(caller, batch);
                }
                if (agentsFetched >= syncConfig.maxEntities()) {
                    break;
                }
            }
        }
        entitiesUpserted += flush(caller, batch);
        return new Output(usersFetched, agentsFetched, entitiesUpserted);
    }

    /**
     * Lazily pages items out of the given fetcher and flattens them into a single iteration.
     * Termination is bounded — not an open {@code while (true)}: it stops once a page comes back
     * empty or once every item reported by {@code totalCount} has been handed out, so it never
     * fetches a page it doesn't need.
     */
    private static <T> Iterable<T> pageThrough(PageFetcher<T> fetcher, int pageSize) {
        return () ->
            new Iterator<>() {
                private int page = 0;
                private int fetched = 0;
                private long totalCount = Long.MAX_VALUE;
                private Iterator<T> current = Collections.emptyIterator();

                @Override
                public boolean hasNext() {
                    while (!current.hasNext() && fetched < totalCount) {
                        Page<T> p = fetcher.fetch(page++, pageSize);
                        totalCount = p.totalCount();
                        if (p.items().isEmpty()) {
                            return false;
                        }
                        current = p.items().iterator();
                    }
                    return current.hasNext();
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    fetched++;
                    return current.next();
                }
            };
    }

    @FunctionalInterface
    private interface PageFetcher<T> {
        Page<T> fetch(int page, int size);
    }

    private record Page<T>(List<T> items, long totalCount) {}

    private int flush(AuthzCallerContext caller, List<CreateOrReplaceAuthzEntityCommand> batch) {
        if (batch.isEmpty()) {
            return 0;
        }
        authzEntityAdminApi.bulkUpsert(caller, List.copyOf(batch));
        int count = batch.size();
        batch.clear();
        return count;
    }

    private CreateOrReplaceAuthzEntityCommand userCommand(String environmentId, AmUser user) {
        String sub = computeSub(user);
        // Map.copyOf (inside the command) rejects null values, so only put attributes AM populated.
        Map<String, Object> attributes = new LinkedHashMap<>();

        // Present synced principals as the "user" kind in the UI (Type column + user.<sub> entity id)
        // without changing the stored entityId, which stays the bare sub so the PEP's
        // Principal::"<sub>" still matches at eval time.
        attributes.put("_kind", "user");

        // Surface the resolved token sub as a visible property too (it is also the entityId).
        attributes.put("sub", sub);
        if (user.email() != null) {
            attributes.put("email", user.email());
        }
        if (user.username() != null) {
            attributes.put("username", user.username());
        }
        if (user.displayName() != null) {
            attributes.put("displayName", user.displayName());
        }
        if (user.enabled() != null) {
            attributes.put("enabled", user.enabled());
        }

        // Membership: the user's parents are its AM group + role ids. Replaced wholesale on every
        // upsert, so a membership dropped in AM disappears from parents on the next sync.
        List<String> parents = new ArrayList<>(user.groups());
        parents.addAll(user.roles());

        return new CreateOrReplaceAuthzEntityCommand(environmentId, sub, AuthzEntityKind.PRINCIPAL, null, attributes, parents, SOURCE);
    }

    private CreateOrReplaceAuthzEntityCommand groupCommand(String environmentId, AmGroup group) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("_kind", "group");
        attributes.put("name", group.name() != null ? group.name() : group.id());
        return new CreateOrReplaceAuthzEntityCommand(environmentId, group.id(), AuthzEntityKind.PRINCIPAL, null, attributes, List.of(), SOURCE);
    }

    private CreateOrReplaceAuthzEntityCommand roleCommand(String environmentId, AmRole role) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("_kind", "role");
        attributes.put("name", role.name() != null ? role.name() : role.id());
        return new CreateOrReplaceAuthzEntityCommand(environmentId, role.id(), AuthzEntityKind.PRINCIPAL, null, attributes, List.of(), SOURCE);
    }

    private CreateOrReplaceAuthzEntityCommand agentCommand(String environmentId, String domain, AmAgent agent) {
        // The entity id must equal what the PEP derives at request time from domain + token client_id.
        String entityId = AgentEntityId.derive(domain, agent.clientId());
        // Map.copyOf (inside the command) rejects null values, so only put attributes AM populated.
        Map<String, Object> attributes = new LinkedHashMap<>();
        // "agent-identity" (not "agent"): the UI's entity-kind registry maps this to the AgentIdentity
        // type, matching the agent-identity.<domain>.<uuid> entity id. "agent" is the catalog/A2A kind.
        attributes.put("_kind", "agent-identity");
        attributes.put("clientId", agent.clientId());
        attributes.put("domain", domain);
        if (agent.name() != null) {
            attributes.put("name", agent.name());
        }
        if (agent.agentType() != null) {
            attributes.put("agentType", agent.agentType());
        }
        // Agent identities are generic and user-independent: no parents. User-specific rules belong in
        // policy conditions, not the entity graph.
        return new CreateOrReplaceAuthzEntityCommand(environmentId, entityId, AuthzEntityKind.PRINCIPAL, null, attributes, List.of(), SOURCE);
    }

    // Mirrors AM's SubjectManagerV2: the token `sub` a V2 domain issues is an MD5-based UUID of
    // "source:externalId", not the user id. PRINCIPAL entities must be keyed on that `sub` for the
    // PEP's Principal::"<sub>" to match at eval time. When either source or externalId is absent
    // (extension-grant users, and V1 domains) AM falls back to the user id.
    static String computeSub(AmUser user) {
        if (user.source() == null || user.externalId() == null) {
            return user.id();
        }
        String internalSub = user.source() + ":" + user.externalId();
        return UUID.nameUUIDFromBytes(internalSub.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public record Input(AuthzCallerContext caller, AmConnection connection) {}

    public record Output(int usersFetched, int agentsFetched, int entitiesUpserted) {}

    public record SyncConfig(int maxEntities, int pageSize, int batchSize) {}
}
