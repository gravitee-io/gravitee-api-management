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
package io.gravitee.gamma.authorization.core.am.service_provider;

import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.gamma.authorization.core.am.model.AmUserPage;

/**
 * Port for paging users out of an AM domain. The infra implementation owns the AM SDK details
 * (client construction, default org/env scoping, timestamp handling).
 */
public interface AmUserClient {
    /**
     * Opens a paging session bound to one AM connection. The implementation builds a single
     * underlying SDK client and reuses it across every page of the run; the caller must
     * {@link Session#close()} it (try-with-resources) to release the HTTP client.
     */
    Session openSession(AmConnection connection);

    /** A connection-bound cursor over an AM domain's users. Not thread-safe; use within one run. */
    interface Session extends AutoCloseable {
        AmUserPage fetchUsers(int page, int size);

        @Override
        void close();
    }
}
