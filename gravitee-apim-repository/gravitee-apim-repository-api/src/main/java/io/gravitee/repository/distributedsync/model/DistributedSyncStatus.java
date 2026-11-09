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
 * Whether a gateway cluster takes its configuration from the control plane or only from the distributed sync repository.
 *
 * @author GraviteeSource Team
 */
public enum DistributedSyncStatus {
    /** The primary syncs from the control plane and secondaries read the distributed sync repository. */
    CONNECTED,
    /** A pause was requested. Local changes stay locked until the primary confirms {@link #PAUSED}. */
    PAUSING,
    /** Every node reads the distributed sync repository and local changes are allowed. */
    PAUSED,
    /** The primary replays control plane changes made since its last successful sync. */
    RESUMING,
}
