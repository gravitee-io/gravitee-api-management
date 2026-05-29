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
package io.gravitee.gamma.authorization.am;

import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.am.sdk.management.api.UserApi;
import io.gravitee.am.sdk.management.model.User;
import io.gravitee.am.sdk.management.model.UserPage;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.api.AuthzEntityAdminApi;
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
 * stale entities. Runs on a worker thread (see {@link AmSyncJobManager}).
 */
public class AmUserSyncOrchestrator {

    // AM resolves the domain by id alone; org/env only scope the service-account permission check,
    // so the well-known default AM org/env are used (see plan: AM UsersResource#list).
    static final String AM_DEFAULT_ORGANIZATION = "DEFAULT";
    static final String AM_DEFAULT_ENVIRONMENT = "DEFAULT";
    static final String SOURCE = "gravitee_am";
    static final int PAGE_SIZE = 1000;
    static final int BATCH_SIZE = 500;

    private final AmSdkUserClientFactory clientFactory;
    private final AuthzEntityAdminApi authzEntityAdminApi;

    public AmUserSyncOrchestrator(AmSdkUserClientFactory clientFactory, AuthzEntityAdminApi authzEntityAdminApi) {
        this.clientFactory = clientFactory;
        this.authzEntityAdminApi = authzEntityAdminApi;
    }

    public record Result(int usersFetched, int entitiesUpserted) {}

    public Result run(AuthzCallerContext caller, AmConnection connection) {
        UserApi userApi = clientFactory.userApi(connection);
        String domainId = connection.defaultDomainId();

        int page = 0;
        int usersFetched = 0;
        int entitiesUpserted = 0;
        List<CreateOrReplaceAuthzEntityCommand> batch = new ArrayList<>();

        while (true) {
            UserPage userPage = AmSdkInvocations.await(
                userApi.listUsers(AM_DEFAULT_ORGANIZATION, AM_DEFAULT_ENVIRONMENT, domainId, null, null, page, PAGE_SIZE)
            );
            List<User> data = userPage.getData() == null ? List.of() : userPage.getData();
            if (data.isEmpty()) {
                break;
            }
            for (User user : data) {
                usersFetched++;
                batch.add(toCommand(caller.environmentId(), user));
                if (batch.size() >= BATCH_SIZE) {
                    entitiesUpserted += flush(caller, batch);
                }
            }
            long totalCount = userPage.getTotalCount() == null ? 0 : userPage.getTotalCount();
            if (usersFetched >= totalCount) {
                break;
            }
            page++;
        }
        entitiesUpserted += flush(caller, batch);
        return new Result(usersFetched, entitiesUpserted);
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

    private CreateOrReplaceAuthzEntityCommand toCommand(String environmentId, User user) {
        // Map.copyOf (inside the command) rejects null values, so only put attributes AM populated.
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (user.getEmail() != null) {
            attributes.put("email", user.getEmail());
        }
        if (user.getUsername() != null) {
            attributes.put("username", user.getUsername());
        }
        if (user.getDisplayName() != null) {
            attributes.put("displayName", user.getDisplayName());
        }
        if (user.getEnabled() != null) {
            attributes.put("enabled", user.getEnabled());
        }
        return new CreateOrReplaceAuthzEntityCommand(environmentId, computeSub(user), AuthzEntityKind.PRINCIPAL, attributes, List.of(), SOURCE);
    }

    // Mirrors AM's SubjectManagerV2: the token `sub` a V2 domain issues is an MD5-based UUID of
    // "source:externalId", not the user id. PRINCIPAL entities must be keyed on that `sub` for the
    // PEP's Principal::"<sub>" to match at eval time. When source is absent (extension-grant users,
    // and V1 domains) AM falls back to the user id.
    static String computeSub(User user) {
        if (user.getSource() == null) {
            return user.getId();
        }
        String internalSub = user.getSource() + ":" + user.getExternalId();
        return UUID.nameUUIDFromBytes(internalSub.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
