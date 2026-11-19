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
package io.gravitee.gamma.rest.core.observability.filter.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.StaticFilters;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.EndUserDisplayPort;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

/**
 * The organization decides whether the end user of a call is shown at all ({@code logging.user.displayed}, off by
 * default). Every read that can name the {@code USER} field asks here, so the catalog, the queries and the logs agree.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class EndUserDisplayDomainService {

    private final EndUserDisplayPort endUserDisplay;

    public boolean displaysEndUser(String organizationId) {
        return endUserDisplay.displaysEndUser(organizationId);
    }

    /** Refuses fields naming the end user while the organization does not display it; other fields pass. */
    public void requireDisplayedIfNamed(String organizationId, Stream<String> fieldNames) {
        if (fieldNames.anyMatch(StaticFilters::isEndUser) && !displaysEndUser(organizationId)) {
            throw UnsupportedObservabilityFilterException.endUserNotDisplayed(StaticFilters.USER.name());
        }
    }
}
