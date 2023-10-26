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
package io.gravitee.apim.core.log.usecase;

import io.gravitee.apim.core.log.crud_service.MessageLogCrudService;
import io.gravitee.apim.core.log.model.AggregatedMessageLog;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import java.util.List;
import java.util.Optional;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SearchMessageLogsUsecase {

    private final MessageLogCrudService messageLogCrudService;

    public SearchMessageLogsUsecase(MessageLogCrudService messageLogCrudService) {
        this.messageLogCrudService = messageLogCrudService;
    }

    public Output execute(Input input) {
        var pageable = input.pageable.orElse(new PageableImpl(1, 20));

        var response = messageLogCrudService.searchApiMessageLog(input.apiId(), input.requestId(), pageable);
        return mapToResponse(response);
    }

    private Output mapToResponse(SearchLogsResponse<AggregatedMessageLog> logs) {
        var total = logs.total();
        var data = logs.logs();

        return new Output(total, data);
    }

    public record Input(String apiId, String requestId, Optional<Pageable> pageable) {
        public Input(String apiId, String requestId) {
            this(apiId, requestId, Optional.empty());
        }

        public Input(String apiId, String requestId, Pageable pageable) {
            this(apiId, requestId, Optional.of(pageable));
        }
    }

    public record Output(long total, List<AggregatedMessageLog> data) {}
}
