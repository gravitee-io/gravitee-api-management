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

import io.gravitee.repository.management.model.SubscriptionReferenceType;
import java.util.Collection;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder(toBuilder = true)
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class SubscriptionCriteria {

    private final Collection<String> ids;

    private final Set<String> environments;

    private final Collection<String> excludedApis;

    private final Collection<String> apis;

    private final Collection<String> referenceIds;

    private final SubscriptionReferenceType referenceType;

    private final Collection<String> plans;

    private final Collection<String> statuses;

    private final Collection<String> applications;

    private final Collection<String> planSecurityTypes;

    @Builder.Default
    private final long from = -1;

    @Builder.Default
    private final long to = -1;

    @Builder.Default
    private final long endingAtAfter = -1;

    @Builder.Default
    private final long endingAtBefore = -1;

    private final boolean includeWithoutEnd;

    private final String clientId;
}
