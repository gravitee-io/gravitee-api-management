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
package io.gravitee.gamma.authorization.am;

import java.time.Instant;

/**
 * Immutable snapshot of an AM user-sync job for one organization. Held in memory by
 * {@link AmSyncJobManager} and lost on restart (acceptable — a sync can simply be re-triggered).
 */
public record AmSyncJobState(
    String jobId,
    AmSyncStatus status,
    int usersFetched,
    int entitiesUpserted,
    String error,
    Instant startedAt,
    Instant completedAt
) {
    public static AmSyncJobState running(String jobId, Instant startedAt) {
        return new AmSyncJobState(jobId, AmSyncStatus.RUNNING, 0, 0, null, startedAt, null);
    }

    public AmSyncJobState completed(int usersFetched, int entitiesUpserted, Instant completedAt) {
        return new AmSyncJobState(jobId, AmSyncStatus.COMPLETED, usersFetched, entitiesUpserted, null, startedAt, completedAt);
    }

    public AmSyncJobState failed(String error, Instant completedAt) {
        return new AmSyncJobState(jobId, AmSyncStatus.FAILED, usersFetched, entitiesUpserted, error, startedAt, completedAt);
    }
}
