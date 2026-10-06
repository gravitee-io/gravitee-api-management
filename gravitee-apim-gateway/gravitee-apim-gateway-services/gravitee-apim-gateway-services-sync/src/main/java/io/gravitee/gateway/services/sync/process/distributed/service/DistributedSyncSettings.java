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
package io.gravitee.gateway.services.sync.process.distributed.service;

/**
 * @param tombstoneRetentionMs how long removal events are kept in the distributed repository; {@code 0} keeps them forever
 * @param tombstonePurgeIntervalMs how often the primary purges removal events older than the retention
 * @param topology description of what this node synchronizes (sharding tags, tenant, organizations, environments)
 * @param primaryStaleAfterMs how old the primary heartbeat may get before secondaries report it as stale
 */
public record DistributedSyncSettings(
    long tombstoneRetentionMs,
    long tombstonePurgeIntervalMs,
    String topology,
    long primaryStaleAfterMs,
    boolean fencingEnabled
) {
    public static final long DEFAULT_TOMBSTONE_RETENTION_MS = 7L * 24 * 60 * 60 * 1000;
    public static final long DEFAULT_TOMBSTONE_PURGE_INTERVAL_MS = 60L * 60 * 1000;
    public static final long DEFAULT_PRIMARY_STALE_AFTER_MS = 2L * 60 * 1000;

    public DistributedSyncSettings(long tombstoneRetentionMs, long tombstonePurgeIntervalMs) {
        this(tombstoneRetentionMs, tombstonePurgeIntervalMs, null, DEFAULT_PRIMARY_STALE_AFTER_MS, true);
    }

    public DistributedSyncSettings(long tombstoneRetentionMs, long tombstonePurgeIntervalMs, String topology, long primaryStaleAfterMs) {
        this(tombstoneRetentionMs, tombstonePurgeIntervalMs, topology, primaryStaleAfterMs, true);
    }

    public static DistributedSyncSettings defaults() {
        return new DistributedSyncSettings(DEFAULT_TOMBSTONE_RETENTION_MS, DEFAULT_TOMBSTONE_PURGE_INTERVAL_MS);
    }
}
