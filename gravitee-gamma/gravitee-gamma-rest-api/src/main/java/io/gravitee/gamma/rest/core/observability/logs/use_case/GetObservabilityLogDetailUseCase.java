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
import io.gravitee.gamma.rest.core.observability.logs.model.LogDetail;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort;
import java.util.Optional;
import lombok.AllArgsConstructor;

/**
 * Fetches a single merged log detail by request id and API scope. The heavy counterpart of
 * {@link SearchObservabilityLogsUseCase}: combines enriched metadata from the {@code v4-metrics}
 * index and HTTP payloads (headers + body) from the {@code v4-log} index into one
 * {@link LogDetail}.
 *
 * <p>Authorization ({@code API_LOG[READ]} with 404 collapse) is enforced at the REST layer before
 * this use case is invoked — the use case assumes the caller is allowed to read logs for the given
 * API.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class GetObservabilityLogDetailUseCase {

    private final ObservabilityLogsDataPort logsDataPort;

    public record Input(String organizationId, String environmentId, String apiId, String requestId) {}

    public record Output(Optional<LogDetail> detail) {}

    public Output execute(Input input) {
        var detail = logsDataPort.getLogDetail(input.organizationId, input.environmentId, input.apiId, input.requestId);
        return new Output(detail);
    }
}
