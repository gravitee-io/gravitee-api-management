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
package io.gravitee.repository.management.api.search;

import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointStatus;
import io.gravitee.repository.management.model.AccessPointTarget;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Builder(toBuilder = true)
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class AccessPointCriteria {

    @Builder.Default
    private final long from = -1;

    @Builder.Default
    private final long to = -1;

    private final AccessPointReferenceType referenceType;

    /**
     * @deprecated Use {@link #targets} instead.
     * Keep for backward compatibility with bridge for gateway <= 4.7.x
     */
    @Deprecated
    private final AccessPointTarget target;

    private final List<AccessPointTarget> targets;

    private final Set<String> referenceIds;

    private final AccessPointStatus status;
}
