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
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.log.SearchLogResponse;
import io.gravitee.rest.api.model.v4.log.message.BaseMessageLog;
import java.util.List;
import java.util.Optional;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SearchMessageLogUsecase {

    private final MessageLogCrudService messageLogCrudService;

    public SearchMessageLogUsecase(MessageLogCrudService messageLogCrudService) {
        this.messageLogCrudService = messageLogCrudService;
    }

    public Output execute(Input input) {
        var pageable = input.pageable.orElse(new PageableImpl(1, 20));

        var response = messageLogCrudService.searchApiMessageLog(input.apiId(), input.requestId(), pageable);
        return mapToResponse(response);
    }

    private Output mapToResponse(SearchLogResponse<BaseMessageLog> logs) {
        var total = logs.total();
        var data = logs.logs();

        return new Output(total, data);
    }

    public record Input(String apiId, String requestId, Optional<Pageable> pageable) {
        public Input(String apiId, String requestId) {
            this(apiId, requestId, Optional.empty());
        }
<<<<<<< HEAD:gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/log/usecase/SearchMessageLogUsecase.java
<<<<<<< HEAD:gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/log/usecase/SearchMessageLogUsecase.java

        public Request(String apiId, String requestId, String userId, Pageable pageable) {
            this(apiId, requestId, userId, Optional.of(pageable));
=======
        public Request(String apiId, String requestId, Pageable pageable) {
=======
        public Input(String apiId, String requestId, Pageable pageable) {
>>>>>>> 30bbe07027 (refactor: rename logs use cases Request and Response to Input and Output):gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/usecase/log/SearchMessageLogUsecase.java
            this(apiId, requestId, Optional.of(pageable));
>>>>>>> 424a0fd57a (refactor: remove unused userId parameter from LogsResource):gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/usecase/log/SearchMessageLogUsecase.java
        }
    }

    public record Output(long total, List<BaseMessageLog> data) {}
}
