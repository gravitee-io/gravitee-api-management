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
import io.gravitee.gamma.authorization.core.am.service_provider.AmUserClient;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.service.CreateOrReplaceAuthzEntityCommand;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pages every user out of the AM domain on the connection and upserts each as a PRINCIPAL authz
 * entity via {@link AuthzEntityAdminApi#bulkUpsert}. Upsert-only: users removed in AM are left as
 * stale entities. Invoked on a worker thread by the infra job manager.
 */
public class SyncAmUsersUseCase {

    static final String SOURCE = "gravitee_am";
    static final int PAGE_SIZE = 1000;
    static final int BATCH_SIZE = 500;

    private final AmUserClient amUserClient;
    private final AuthzEntityAdminApi authzEntityAdminApi;

    public SyncAmUsersUseCase(AmUserClient amUserClient, AuthzEntityAdminApi authzEntityAdminApi) {
        this.amUserClient = amUserClient;
        this.authzEntityAdminApi = authzEntityAdminApi;
    }

    public record Input(AuthzCallerContext caller, AmConnection connection) {}

    public record Output(int usersFetched, int entitiesUpserted) {}

    public Output execute(Input input) {
        AuthzCallerContext caller = input.caller();
        AmConnection connection = input.connection();

        int page = 0;
        int usersFetched = 0;
        int entitiesUpserted = 0;
        List<CreateOrReplaceAuthzEntityCommand> batch = new ArrayList<>();

        while (true) {
            AmUserPage userPage = amUserClient.fetchUsers(connection, page, PAGE_SIZE);
            if (userPage.users().isEmpty()) {
                break;
            }
            for (AmUser user : userPage.users()) {
                usersFetched++;
                batch.add(toCommand(caller.environmentId(), user));
                if (batch.size() >= BATCH_SIZE) {
                    entitiesUpserted += flush(caller, batch);
                }
            }
            if (usersFetched >= userPage.totalCount()) {
                break;
            }
            page++;
        }
        entitiesUpserted += flush(caller, batch);
        return new Output(usersFetched, entitiesUpserted);
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
}
