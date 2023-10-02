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
package io.gravitee.apim.usecase.log;

import io.gravitee.apim.crud_service.analytics.log.MessageLogCrudService;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.log.SearchLogResponse;
import io.gravitee.rest.api.model.v4.log.message.BaseMessageLog;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
public class SearchMessageLogUsecase {

    private final MessageLogCrudService messageLogCrudService;

    public SearchMessageLogUsecase(MessageLogCrudService messageLogCrudService) {
        this.messageLogCrudService = messageLogCrudService;
    }

    public Response execute(Request request) {
        var pageable = request.pageable.orElse(new PageableImpl(1, 20));

        var response = messageLogCrudService.searchApiMessageLog(request.apiId(), request.requestId(), pageable);
        return mapToResponse(response);
    }

    private Response mapToResponse(SearchLogResponse<BaseMessageLog> logs) {
        var total = logs.total();
        var data = logs.logs();

        return new Response(total, data);
    }

    public record Request(String apiId, String requestId, Optional<Pageable> pageable) {
        public Request(String apiId, String requestId) {
            this(apiId, requestId, Optional.empty());
        }
        public Request(String apiId, String requestId, Pageable pageable) {
            this(apiId, requestId, Optional.of(pageable));
        }
    }

    public record Response(long total, List<BaseMessageLog> data) {}
}
