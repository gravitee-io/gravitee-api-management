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
package io.gravitee.apim.core.log.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.log.crud_service.NativeApiLogCrudService;
import io.gravitee.apim.core.log.model.NativeApiLog;
import io.gravitee.apim.core.log.model.NativeConnectionStatus;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Set;

@UseCase
public class NativeApiLogSearchUseCase {

    private final NativeApiLogCrudService nativeApiLogCrudService;

    public NativeApiLogSearchUseCase(NativeApiLogCrudService nativeApiLogCrudService) {
        this.nativeApiLogCrudService = nativeApiLogCrudService;
    }

    public record Input(
        ExecutionContext executionContext,
        String apiId,
        Long from,
        Long to,
        Set<String> applicationIds,
        Set<String> planIds,
        Set<NativeConnectionStatus> connectionStatuses,
        int page,
        int size
    ) {}

    public record Output(SearchLogsResponse<NativeApiLog> response) {}

    public Output execute(Input input) {
        var filter = NativeApiLogCrudService.Filter.builder()
            .from(input.from())
            .to(input.to())
            .applicationIds(input.applicationIds())
            .planIds(input.planIds())
            .connectionStatuses(input.connectionStatuses())
            .build();
        return new Output(nativeApiLogCrudService.searchLogs(input.executionContext(), input.apiId(), filter, input.page(), input.size()));
    }
}
