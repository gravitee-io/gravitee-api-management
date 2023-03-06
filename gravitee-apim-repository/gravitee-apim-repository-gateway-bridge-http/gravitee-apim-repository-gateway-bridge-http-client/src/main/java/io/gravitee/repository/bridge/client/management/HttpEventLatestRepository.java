/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.bridge.client.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.bridge.client.http.HttpResponse;
import io.gravitee.repository.bridge.client.utils.BodyCodecs;
import io.gravitee.repository.bridge.client.utils.ExcludeMethodFromGeneratedCoverage;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Event;
import io.vertx.ext.web.codec.BodyCodec;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpEventLatestRepository extends AbstractRepository implements EventLatestRepository {

    @Override
    public Event createOrPatch(Event event) throws TechnicalException {
        return blockingGet(post("/eventsLatest/_createOrPatch", BodyCodec.json(Event.class)).send(event)).payload();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public void delete(String eventId) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public List<Event> search(EventCriteria criteria, Event.EventProperties group, Long page, Long size) {
        try {
            HttpResponse<List<Event>> httpResponse = blockingGet(
                post("/eventsLatest/_search", BodyCodecs.list(Event.class))
                    .addQueryParam("group", group.name())
                    .addQueryParam("page", Long.toString(page))
                    .addQueryParam("size", Long.toString(size))
                    .send(criteria)
            );
            if (httpResponse.statusCode() == HttpStatusCode.NOT_FOUND_404) {
                httpResponse =
                    blockingGet(
                        post("/events/_searchLatest", BodyCodecs.list(Event.class))
                            .addQueryParam("group", group.name())
                            .addQueryParam("page", Long.toString(page))
                            .addQueryParam("size", Long.toString(size))
                            .send(criteria)
                    );
            }
            return httpResponse.payload();
        } catch (TechnicalException te) {
            throw new IllegalStateException(te);
        }
    }
}
