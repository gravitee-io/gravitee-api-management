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
package io.gravitee.repository.distributedsync.api.search;

import io.gravitee.repository.distributedsync.model.DistributedEventType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Identifies one distributed event, the way {@link io.gravitee.repository.distributedsync.api.DistributedEventRepository#createOrUpdate}
 * stores it. The reference is part of the identity: an event stored with one is only found with the same reference.
 *
 * @author GraviteeSource Team
 */
@Getter
@EqualsAndHashCode
@ToString
public class DistributedEventKey {

    private final String clusterId;
    /** Optional, given with {@link #refId}. For a subscription: {@code API}. */
    private final DistributedEventType refType;
    /** Optional, given with {@link #refType}. For a subscription: its API or API Product id. */
    private final String refId;
    private final DistributedEventType type;
    private final String id;

    @Builder
    private DistributedEventKey(
        final String clusterId,
        final DistributedEventType refType,
        final String refId,
        final DistributedEventType type,
        final String id
    ) {
        if (clusterId == null || clusterId.isBlank()) {
            throw new IllegalArgumentException("A distributed event key needs a clusterId");
        }
        if (type == null) {
            throw new IllegalArgumentException("A distributed event key needs a type");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("A distributed event key needs an id");
        }
        if ((refType == null) != (refId == null)) {
            throw new IllegalArgumentException("A distributed event key takes refType and refId together, or neither");
        }
        this.clusterId = clusterId;
        this.refType = refType;
        this.refId = refId;
        this.type = type;
        this.id = id;
    }

    public boolean hasReference() {
        return refType != null;
    }
}
