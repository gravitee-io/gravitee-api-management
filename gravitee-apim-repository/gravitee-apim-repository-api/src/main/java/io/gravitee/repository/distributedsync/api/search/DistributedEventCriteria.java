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
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class DistributedEventCriteria {

    private final DistributedEventType type;

    private final Set<DistributedSyncAction> syncActions;

    @Builder.Default
    private final long from = -1;

    @Builder.Default
    private final long to = -1;
}
