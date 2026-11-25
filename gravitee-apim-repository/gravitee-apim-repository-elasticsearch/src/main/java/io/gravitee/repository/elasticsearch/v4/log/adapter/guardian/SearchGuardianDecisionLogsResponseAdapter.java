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
package io.gravitee.repository.elasticsearch.v4.log.adapter.guardian;

import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionLogResponses;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLog;
import java.util.Optional;

/**
 * @author GraviteeSource Team
 */
public final class SearchGuardianDecisionLogsResponseAdapter {

    private SearchGuardianDecisionLogsResponseAdapter() {}

    public static LogResponse<GuardianDecisionLog> adapt(SearchResponse response) {
        return DecisionLogResponses.page(response, GuardianDecisionLogSourceMapper::from);
    }

    public static Optional<GuardianDecisionLog> adaptFirst(SearchResponse response) {
        return DecisionLogResponses.first(response, GuardianDecisionLogSourceMapper::from);
    }
}
