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
package io.gravitee.gamma.rest.core.observability.capabilities.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.observability.analytics.model.DataKind;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.capabilities.model.ObservabilityCapabilities;
import io.gravitee.gamma.rest.core.observability.capabilities.port.service_provider.DashboardPermissionPort;
import io.gravitee.gamma.rest.core.observability.capabilities.port.service_provider.DashboardPermissionPort.Action;
import lombok.AllArgsConstructor;

/**
 * The observability capabilities of the caller in an environment:
 *
 * <ul>
 *   <li>logs and traces, when they read the requests of at least one API;</li>
 *   <li>dashboards, when they read the aggregates of at least one API or hold {@code ENVIRONMENT_DASHBOARD[READ]};</li>
 *   <li>dashboard writes, with {@code ENVIRONMENT_DASHBOARD[CREATE]} or {@code [UPDATE]}.</li>
 * </ul>
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class GetObservabilityCapabilitiesUseCase {

    private final ObservabilityAnalyticsDataPort analyticsDataPort;
    private final DashboardPermissionPort dashboardPermissionPort;

    public record Input(String organizationId, String environmentId) {}

    public record Output(ObservabilityCapabilities capabilities) {}

    public Output execute(Input input) {
        var readsRequests = readsAnyApi(input, DataKind.PER_REQUEST);
        // The permission first: it is one check, where the aggregates are a search of the readable APIs.
        var readsDashboards = holds(input, Action.READ) || readsAnyApi(input, DataKind.AGGREGATES);
        var writesDashboards = holds(input, Action.CREATE) || holds(input, Action.UPDATE);
        return new Output(new ObservabilityCapabilities(readsRequests, readsRequests, readsDashboards, writesDashboards));
    }

    private boolean readsAnyApi(Input input, DataKind kind) {
        return !analyticsDataPort.loadAccessibleApis(input.organizationId(), input.environmentId(), kind).isEmpty();
    }

    private boolean holds(Input input, Action action) {
        return dashboardPermissionPort.holds(input.organizationId(), input.environmentId(), action);
    }
}
