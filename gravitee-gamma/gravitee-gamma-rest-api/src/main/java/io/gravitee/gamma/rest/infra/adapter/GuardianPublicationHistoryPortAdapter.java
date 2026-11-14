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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.GuardianPublicationHistoryPort;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GuardianPublicationHistoryPortAdapter implements GuardianPublicationHistoryPort {

    private final EventQueryService eventQueryService;

    @Override
    public Page<Event> publications(String environmentId, String genericEventId, int pageNumber, int pageSize) {
        var response = eventQueryService.search(
            new EventQueryService.SearchQuery(
                environmentId,
                Optional.empty(),
                Optional.empty(),
                List.of(EventType.PUBLISH_GENERIC_EVENT),
                Map.of(Event.EventProperties.GENERIC_EVENT_ID, genericEventId),
                Optional.empty(),
                Optional.empty()
            ),
            new PageableImpl(pageNumber, pageSize)
        );
        return new Page<>(response.events(), pageNumber, response.events().size(), response.total());
    }
}
