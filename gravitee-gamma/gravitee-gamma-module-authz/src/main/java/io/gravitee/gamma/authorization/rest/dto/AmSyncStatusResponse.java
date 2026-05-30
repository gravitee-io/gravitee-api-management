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
package io.gravitee.gamma.authorization.rest.dto;

import io.gravitee.apim.core.async_job.model.AsyncJob;
import java.time.ZonedDateTime;

/**
 * Status of an AM user-sync job. {@code status} is the {@link AsyncJob.Status} name
 * (PENDING / SUCCESS / ERROR / TIMEOUT). {@code entitiesUpserted} is the count of PRINCIPAL
 * entities synced (populated on success). {@code completedAt} is set once the job is final.
 */
public record AmSyncStatusResponse(String jobId, String status, Long entitiesUpserted, String error, ZonedDateTime completedAt) {
    public static AmSyncStatusResponse from(AsyncJob job) {
        boolean isFinal = job.getStatus().isFinal();
        return new AmSyncStatusResponse(
            job.getId(),
            job.getStatus().name(),
            job.getUpperLimit(),
            job.getErrorMessage(),
            isFinal ? job.getUpdatedAt() : null
        );
    }
}
