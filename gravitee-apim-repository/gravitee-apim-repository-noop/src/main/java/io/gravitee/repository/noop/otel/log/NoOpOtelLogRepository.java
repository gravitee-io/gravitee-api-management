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
package io.gravitee.repository.noop.otel.log;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.otel.log.api.OtelLogRepository;
import io.gravitee.repository.otel.log.model.OtelLogRecord;
import io.gravitee.repository.otel.log.model.OtelLogSearchCriteria;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

/**
 * No-op {@link OtelLogRepository} for the {@code OTEL_LOGS} scope. Wired when an operator sets
 * {@code repositories.otel-logs.type=none} — typical for deployments that haven't enabled span-event
 * / payload-log retrieval yet, or that haven't shipped {@code gravitee-reporter-otel} on the gateway.
 * Returns empty results so trace-detail consumers render spans without their events / payload logs
 * rather than failing.
 *
 * @author GraviteeSource Team
 */
public class NoOpOtelLogRepository implements OtelLogRepository {

    @Override
    public Single<List<OtelLogRecord>> findLogs(QueryContext queryContext, OtelLogSearchCriteria criteria) {
        return Single.just(List.of());
    }
}
