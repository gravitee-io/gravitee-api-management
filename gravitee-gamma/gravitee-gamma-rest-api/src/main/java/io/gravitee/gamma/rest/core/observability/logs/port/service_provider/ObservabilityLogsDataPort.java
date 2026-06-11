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
package io.gravitee.gamma.rest.core.observability.logs.port.service_provider;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.logs.model.LogsPage;
import io.gravitee.gamma.rest.core.observability.logs.model.LogsSearchQuery;
import java.util.List;

/**
 * Core-side port onto the logs data store and the RBAC-scoped API inventory. The infra adapter
 * delegates to the platform's {@code ConnectionLogsCrudService}, {@code UserContextLoader}, and
 * the various name-enrichment services so the core stays unaware of those mechanics.
 *
 * <p>Tenancy / actor scoping for {@link #loadAccessibleApis} is resolved by the adapter from the
 * ambient request context (same source the rest of the rest-api uses).
 *
 * @author GraviteeSource Team
 */
public interface ObservabilityLogsDataPort {
    /**
     * An API the current caller is allowed to read, carrying the definition-level type so the use
     * case can filter by signal-relevant API kinds without leaking repository details.
     */
    record AccessibleApi(String id, String name, ApiType type) {}

    /** Loads all APIs the current caller can read in the given environment. */
    List<AccessibleApi> loadAccessibleApis(String organizationId, String environmentId);

    /**
     * Searches the {@code v4-metrics} index with the pre-scoped query and returns enriched log
     * entries (plan/application/gateway/apiProduct names resolved). Only V4 definition versions are
     * queried.
     */
    LogsPage searchLogs(String organizationId, String environmentId, LogsSearchQuery query);
}
