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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The JSON payload of a local version record: a change made while control plane sync was paused.
 * Records are {@link DistributedEvent}s stored under {@link #recordsClusterId(String)}, so normal sync queries never see them.
 * A {@link DistributedSyncAction#DEPLOY} record is active and an {@link DistributedSyncAction#UNDEPLOY} record is closed.
 *
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
public class DistributedLocalVersion {

    private static final String RECORDS_CLUSTER_ID_SUFFIX = "-break-glass";

    /**
     * {@code deployedAt} of the control plane version edited first. Only a deployment newer than this replaces the local version.
     * For a subscription, the time of the local change, compared with the control plane's {@code updatedAt}.
     */
    private long baseDeployedAt;
    /** Event payload of the control plane version, used by Restore. */
    private String originalPayload;
    /** Event payload of the version currently published. */
    private String localPayload;
    /** {@code false} when the local version stops an API. */
    private boolean running;
    private String change;
    private String author;
    private String reason;
    private long updatedAt;

    public static String recordsClusterId(final String clusterId) {
        return clusterId + RECORDS_CLUSTER_ID_SUFFIX;
    }
}
