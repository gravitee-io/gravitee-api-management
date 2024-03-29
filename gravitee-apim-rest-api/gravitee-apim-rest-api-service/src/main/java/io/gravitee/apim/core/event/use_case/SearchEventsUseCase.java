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
package io.gravitee.apim.core.event.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.model.EventWithInitiator;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import java.util.Optional;

@UseCase
public class SearchEventsUseCase {

    private final EventQueryService eventQueryService;
    private final UserCrudService userCrudService;

    public SearchEventsUseCase(EventQueryService eventQueryService, UserCrudService userCrudService) {
        this.eventQueryService = eventQueryService;
        this.userCrudService = userCrudService;
    }

    public Output execute(Input input) {
        var query = input.query;
        var pageable = input.pageable.orElse(new PageableImpl(1, 10));

        var result = eventQueryService.search(query, pageable);
        return new Output(toEventWithInitiator(result.events()), result.total());
    }

    private List<EventWithInitiator> toEventWithInitiator(List<Event> events) {
        return events
            .stream()
            .map(event -> {
                var initiator = Optional
                    .ofNullable(event.getProperties().get(Event.EventProperties.USER))
                    .map(userId -> userCrudService.findBaseUserById(userId).orElse(BaseUserEntity.builder().id(userId).build()));
                return new EventWithInitiator(event, initiator.orElse(null));
            })
            .toList();
    }

    public record Input(EventQueryService.SearchQuery query, Optional<Pageable> pageable) {
        public Input(EventQueryService.SearchQuery query) {
            this(query, Optional.empty());
        }

        public Input(EventQueryService.SearchQuery query, Pageable pageable) {
            this(query, Optional.of(pageable));
        }
    }

    public record Output(List<EventWithInitiator> data, long total) {}
}
