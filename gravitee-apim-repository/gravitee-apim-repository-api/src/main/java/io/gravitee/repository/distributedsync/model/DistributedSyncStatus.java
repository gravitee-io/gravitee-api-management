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
package io.gravitee.repository.distributedsync.model;

/**
 * Status of the distributed sync catalog published by the primary node.
 */
public enum DistributedSyncStatus {
    /**
     * The catalog is complete: secondaries may start from it.
     */
    ACTIVE,
    /**
     * The primary is rebuilding the catalog (full republish then removal of stale entries): a secondary that never
     * synchronized must wait, otherwise it could start from stale or partial data.
     */
    PAUSED,
}
