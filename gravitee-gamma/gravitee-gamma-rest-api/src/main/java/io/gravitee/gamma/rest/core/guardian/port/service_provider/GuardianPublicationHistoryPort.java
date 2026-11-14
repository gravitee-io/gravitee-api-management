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
package io.gravitee.gamma.rest.core.guardian.port.service_provider;

import io.gravitee.apim.core.event.model.Event;
import io.gravitee.common.data.domain.Page;
import java.util.Optional;

/**
 * The events history of the generic event publications, which the gateways never read.
 *
 * <p>A port rather than the apim {@code EventQueryService}: its paging type lives outside the packages the core may
 * depend on (AGENTS.md §5).
 */
public interface GuardianPublicationHistoryPort {
    /**
     * The publications, never the withdrawals, of one generic event of an environment, newest first.
     *
     * @param genericEventId the id of the generic event, {@code GenericEvent.latestEventId(...)}
     * @param pageNumber 1-based
     */
    Page<Event> publications(String environmentId, String genericEventId, int pageNumber, int pageSize);

    /** One publication of a generic event of an environment; empty for any other event, a withdrawal included. */
    Optional<Event> publication(String environmentId, String genericEventId, String eventId);
}
