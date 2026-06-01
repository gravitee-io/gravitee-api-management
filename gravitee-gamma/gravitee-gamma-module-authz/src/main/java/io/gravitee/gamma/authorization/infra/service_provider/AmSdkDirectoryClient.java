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
package io.gravitee.gamma.authorization.infra.service_provider;

import io.gravitee.am.sdk.management.api.UserApi;
import io.gravitee.am.sdk.management.api.UserApiImpl;
import io.gravitee.am.sdk.management.model.User;
import io.gravitee.am.sdk.management.model.UserPage;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.gamma.authorization.core.am.model.AmUser;
import io.gravitee.gamma.authorization.core.am.model.AmUserPage;
import io.gravitee.gamma.authorization.core.am.service_provider.AmDirectoryClient;
import java.util.List;

/**
 * AM SDK-backed {@link AmDirectoryClient}. Owns the SDK specifics — client construction, the well-known
 * default AM org/env scoping, and mapping the SDK {@link User} onto the SDK-free core {@link AmUser}.
 */
public class AmSdkDirectoryClient implements AmDirectoryClient {

    // AM resolves the domain by id alone; org/env only scope the service-account permission check,
    // so the well-known default AM org/env are used (see AM UsersResource#list).
    private static final String AM_DEFAULT_ORGANIZATION = "DEFAULT";
    private static final String AM_DEFAULT_ENVIRONMENT = "DEFAULT";

    private final AmSdkDirectoryClientFactory clientFactory;

    public AmSdkDirectoryClient(AmSdkDirectoryClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Session openSession(AmConnection connection) {
        // One SDK client (and its Vert.x WebClient connection pool) for the whole run, closed below.
        return new SdkSession(clientFactory.userApi(connection), connection.defaultDomainId());
    }

    private static final class SdkSession implements Session {

        private final UserApi userApi;
        private final String domainId;

        private SdkSession(UserApi userApi, String domainId) {
            this.userApi = userApi;
            this.domainId = domainId;
        }

        @Override
        public AmUserPage fetchUsers(int page, int size) {
            UserPage userPage = AmSdkInvocations.await(
                userApi.listUsers(AM_DEFAULT_ORGANIZATION, AM_DEFAULT_ENVIRONMENT, domainId, null, null, page, size)
            );
            List<AmUser> users = (userPage.getData() == null ? List.<User>of() : userPage.getData())
                .stream()
                .map(AmSdkDirectoryClient::toAmUser)
                .toList();
            long totalCount = userPage.getTotalCount() == null ? 0 : userPage.getTotalCount();
            return new AmUserPage(users, totalCount);
        }

        @Override
        public void close() {
            // Release the underlying Vert.x WebClient pool; building one per page would leak it.
            if (userApi instanceof UserApiImpl impl) {
                impl.getApiClient().getWebClient().close();
            }
        }
    }

    private static AmUser toAmUser(User user) {
        return new AmUser(
            user.getId(),
            user.getSource(),
            user.getExternalId(),
            user.getEmail(),
            user.getUsername(),
            user.getDisplayName(),
            user.getEnabled()
        );
    }
}
