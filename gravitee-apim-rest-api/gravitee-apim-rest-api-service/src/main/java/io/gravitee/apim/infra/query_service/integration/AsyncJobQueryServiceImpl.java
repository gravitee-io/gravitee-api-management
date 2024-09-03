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
package io.gravitee.apim.infra.query_service.integration;

import io.gravitee.apim.core.integration.model.AsyncJob;
import io.gravitee.apim.core.integration.query_service.AsyncJobQueryService;
import io.gravitee.apim.infra.adapter.AsyncJobAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AsyncJobRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AsyncJobQueryServiceImpl extends AbstractService implements AsyncJobQueryService {

    private final AsyncJobRepository asyncJobRepository;

    public AsyncJobQueryServiceImpl(@Lazy AsyncJobRepository asyncJobRepository) {
        this.asyncJobRepository = asyncJobRepository;
    }

    @Override
    public Optional<AsyncJob> findPendingJobFor(String sourceId) {
        try {
            return asyncJobRepository.findPendingJobFor(sourceId).map(AsyncJobAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurred while finding pending AsyncJob for: " + sourceId, e);
        }
    }
}
