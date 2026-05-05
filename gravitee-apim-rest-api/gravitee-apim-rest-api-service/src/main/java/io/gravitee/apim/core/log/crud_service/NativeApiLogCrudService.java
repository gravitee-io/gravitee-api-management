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

import io.gravitee.apim.core.log.model.NativeApiLog;
import io.gravitee.apim.core.log.model.NativeConnectionStatus;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;

public interface NativeApiLogCrudService {
    Optional<NativeApiLog> findLog(ExecutionContext executionContext, String apiId, String requestId, Long from, Long to);

    SearchLogsResponse<NativeApiLog> searchLogs(ExecutionContext executionContext, String apiId, Filter filter, int page, int size);

    @Builder
    record Filter(Long from, Long to, Set<String> applicationIds, Set<String> planIds, Set<NativeConnectionStatus> connectionStatuses) {}
}
