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
package io.gravitee.apim.core.log.crud_service;

import io.gravitee.apim.core.log.model.AuthzDecisionLog;
import io.gravitee.apim.core.log.model.AuthzDecisionLogFilters;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Optional;

/**
 * Reads authorization outcomes from the event-metrics data stream. Separate from
 * {@link ConnectionLogsCrudService} because a decision is not an HTTP exchange and lives in a
 * different index.
 *
 * @author GraviteeSource Team
 */
public interface AuthzDecisionLogsCrudService {
    SearchLogsResponse<AuthzDecisionLog> searchDecisionLogs(
        ExecutionContext executionContext,
        AuthzDecisionLogFilters filters,
        Pageable pageable
    );

    /**
     * One decision by its event id. Batched decisions share a request id, so that is not a key here;
     * the event id is.
     */
    Optional<AuthzDecisionLog> findDecisionLog(ExecutionContext executionContext, String apiId, String eventId);
}
