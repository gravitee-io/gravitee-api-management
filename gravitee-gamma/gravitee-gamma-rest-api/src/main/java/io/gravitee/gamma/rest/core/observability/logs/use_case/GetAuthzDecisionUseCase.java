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
package io.gravitee.gamma.rest.core.observability.logs.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.observability.logs.model.LogEntry;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort;
import java.util.Optional;
import lombok.AllArgsConstructor;

/**
 * Fetches one authorization decision by its event id, for the detail view opened from a decision
 * row. Returns the same shape the search returns, so the caller renders one row's worth of data
 * rather than a second contract.
 *
 * <p>Authorization ({@code API_LOG[READ]} with 404 collapse) is enforced at the REST layer, and the
 * data port additionally resolves the api through the caller's accessible set.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class GetAuthzDecisionUseCase {

    private final ObservabilityLogsDataPort logsDataPort;

    public record Input(String organizationId, String environmentId, String apiId, String eventId) {}

    public record Output(Optional<LogEntry> decision) {}

    public Output execute(Input input) {
        return new Output(logsDataPort.getDecision(input.organizationId, input.environmentId, input.apiId, input.eventId));
    }
}
