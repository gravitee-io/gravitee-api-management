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
import io.gravitee.gamma.authorization.core.am.model.AmUser;
import io.gravitee.gamma.authorization.core.am.model.AmUserPage;
import io.gravitee.gamma.authorization.core.am.service_provider.AmDirectoryClient;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.service.CreateOrReplaceAuthzEntityCommand;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Pages every user out of the AM domain on the connection and upserts each as a PRINCIPAL authz
 * entity via {@link AuthzEntityAdminApi#bulkUpsert}. Upsert-only: users removed in AM are left as
 * stale entities. Invoked on a worker thread by the infra job manager.
 *
 * @author GraviteeSource Team
 */
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

        int usersFetched = 0;
        int entitiesUpserted = 0;
        List<CreateOrReplaceAuthzEntityCommand> batch = new ArrayList<>();

        // Fetching (paging the AM domain) is encapsulated in the cursor below; this loop only owns the
        // business logic of mapping each user to a command and flushing in batches.
        try (AmDirectoryClient.Session session = directoryClient.openSession(input.connection())) {
            for (AmUser user : pageThrough(session, this.syncConfig.pageSize())) {
                usersFetched++;
                batch.add(toCommand(caller.environmentId(), user));
                if (batch.size() >= this.syncConfig.batchSize()) {
                    entitiesUpserted += flush(caller, batch);
                }
                // Cap the sync at the configured ceiling; the lazy cursor stops paging once we break.
                if (usersFetched >= this.syncConfig.maxUsers()) {
                    break;
                }
            }
        }
        entitiesUpserted += flush(caller, batch);
        return new Output(usersFetched, entitiesUpserted);
    }

    /**
     * Lazily pages users out of the session and flattens them into a single iteration. Termination is
     * bounded — not an open {@code while (true)}: it stops once AM returns an empty page or once every
     * user reported by {@code totalCount} has been handed out, so it never fetches a page it doesn't
     * need.
     */
    private Iterable<AmUser> pageThrough(AmDirectoryClient.Session session, int pageSize) {
        return () ->
            new Iterator<>() {
                private int page = 0;
                private int fetched = 0;
                private long totalCount = Long.MAX_VALUE;
                private Iterator<AmUser> current = Collections.emptyIterator();

                @Override
                public boolean hasNext() {
                    while (!current.hasNext() && fetched < totalCount) {
                        AmUserPage userPage = session.fetchUsers(page++, pageSize);
                        totalCount = userPage.totalCount();
                        if (userPage.users().isEmpty()) {
                            return false;
                        }
                        current = userPage.users().iterator();
                    }
                    return current.hasNext();
                }

                @Override
                public AmUser next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    fetched++;
                    return current.next();
                }
            };
    }

    private int flush(AuthzCallerContext caller, List<CreateOrReplaceAuthzEntityCommand> batch) {
        if (batch.isEmpty()) {
            return 0;
        }
        authzEntityAdminApi.bulkUpsert(caller, List.copyOf(batch));
        int count = batch.size();
        batch.clear();
        return count;
    }

    private CreateOrReplaceAuthzEntityCommand toCommand(String environmentId, AmUser user) {
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
        return new CreateOrReplaceAuthzEntityCommand(environmentId, sub, AuthzEntityKind.PRINCIPAL, attributes, List.of(), SOURCE);
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

    public record Output(int usersFetched, int entitiesUpserted) {}

    public record SyncConfig(int maxUsers, int pageSize, int batchSize) {}
}
