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
package io.gravitee.repository.management.api;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.AsyncJob;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface AsyncJobRepository extends CrudRepository<AsyncJob, String> {
    Logger LOGGER = LoggerFactory.getLogger(AsyncJobRepository.class);
    Optional<AsyncJob> findPendingJobFor(String sourceId) throws TechnicalException;

    Page<AsyncJob> search(SearchCriteria criteria, Pageable pageable) throws TechnicalException;

    void delay(String id, Date newDeadLine) throws TechnicalException;

    default Page<AsyncJob> search(SearchCriteria criteria) throws TechnicalException {
        return search(criteria, new PageableBuilder().pageSize(10).pageNumber(0).build());
    }

    record SearchCriteria(
        String environmentId,
        Optional<String> initiatorId,
        Optional<String> type,
        Optional<String> status,
        Optional<String> sourceId
    ) {}

    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;

    default AsyncJob handleDeadLine(AsyncJob job) {
        if (job.isLate()) {
            job.setUpdatedAt(Date.from(TimeProvider.instantNow()));
            job.setStatus("TIMEOUT");
            try {
                return update(job);
            } catch (TechnicalException e) {
                LOGGER.error("An error occurs while updating job {} to timeout", job.getId(), e);
                return job;
            }
        } else {
            return job;
        }
    }
}
