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

import io.gravitee.apim.core.log.model.DecisionLog;
import io.gravitee.apim.core.log.model.DecisionLogFilters;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Optional;

/**
 * Reads the individual records of the {@code decisions} data stream — the detail behind the aggregates the
 * analytics engine serves.
 *
 * <p>Nothing here is tied to one kind of decision point. The family is a filter, so the guardian screens
 * being built now and the human-approval or external-approval screens that follow read through this same
 * service rather than through a class each.
 *
 * <p>Separate from {@link AuthzDecisionLogsCrudService} because the two live in two different data
 * streams: policy-engine decisions carry a whole other document shape and are not comparable row by row.
 *
 * @author GraviteeSource Team
 */
public interface DecisionLogsCrudService {
    /**
     * Records of settled decisions, newest first. A point that holds a call writes once when the hold
     * starts and once when it ends; only the second is read here, so one settled consultation is one row.
     */
    SearchLogsResponse<DecisionLog> searchDecisionLogs(ExecutionContext executionContext, DecisionLogFilters filters, Pageable pageable);

    /**
     * One record by its event id, scoped to an api. Every record of a decision has its own event id — the
     * request and the resolution of an asynchronous decision are two — so this reads the record that id
     * names, open ones included.
     */
    Optional<DecisionLog> findDecisionLog(ExecutionContext executionContext, String apiId, String eventId);
}
