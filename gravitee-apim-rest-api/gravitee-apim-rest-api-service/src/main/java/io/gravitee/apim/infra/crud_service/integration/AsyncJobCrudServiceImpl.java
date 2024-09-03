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
package io.gravitee.apim.infra.crud_service.integration;

import io.gravitee.apim.core.integration.crud_service.AsyncJobCrudService;
import io.gravitee.apim.core.integration.model.AsyncJob;
import io.gravitee.apim.infra.adapter.AsyncJobAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AsyncJobRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class AsyncJobCrudServiceImpl implements AsyncJobCrudService {

    private final AsyncJobRepository asyncJobRepository;

    public AsyncJobCrudServiceImpl(@Lazy AsyncJobRepository asyncJobRepository) {
        this.asyncJobRepository = asyncJobRepository;
    }

    @Override
    public AsyncJob create(AsyncJob job) {
        try {
            var created = asyncJobRepository.create(AsyncJobAdapter.INSTANCE.toRepository(job));
            return AsyncJobAdapter.INSTANCE.toEntity(created);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when creating AsyncJob for integration: " + job.getSourceId(), e);
        }
    }

    @Override
    public Optional<AsyncJob> findById(String id) {
        try {
            return asyncJobRepository.findById(id).map(AsyncJobAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find the AsyncJob: " + id, e);
        }
    }

    @Override
    public AsyncJob update(AsyncJob job) {
        try {
            var updated = asyncJobRepository.update(AsyncJobAdapter.INSTANCE.toRepository(job));
            return AsyncJobAdapter.INSTANCE.toEntity(updated);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurred when updating AsyncJob: " + job.getId(), e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            asyncJobRepository.delete(id);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when deleting AsyncJob: " + id, e);
        }
    }
}
