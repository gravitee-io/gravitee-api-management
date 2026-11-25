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
package io.gravitee.apim.core.event.query_service;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.rest.api.model.EventType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Port for querying the event_latest store, which holds exactly one
 * "current" event per entity (keyed by entity ID).
 */
public interface EventLatestQueryService {
    /**
     * The latest event of a type for each of several entities, in one query.
     *
     * <p>The entity ids are pushed down to the store rather than filtered afterwards, so a caller
     * resolving a page reads that page's rows and not the whole environment's. Entities with no such
     * event are simply absent from the result.</p>
     *
     * @param entityIds   the entity ids (e.g. API Product ids)
     * @param eventType   the event type to look for
     * @param propertyKey the property the event carries the entity id under
     */
    List<Event> findLatestByEntityIds(Set<String> entityIds, EventType eventType, Event.EventProperties propertyKey);

    List<Event> findAllByTypeAndEnvironments(Set<EventType> eventTypes, Set<String> environments, Event.EventProperties groupBy);

    /**
     * Every latest event of the given types, across all environments.
     * <p>
     * Pages are fetched as the stream is consumed, so it can be iterated over any number of events.
     *
     * @param expiresBefore only events expiring before this instant, or <code>null</code> for all
     * @param updatedBefore only events last updated before this instant, or <code>null</code> for all
     */
    Stream<Event> findAll(Set<EventType> eventTypes, Instant expiresBefore, Instant updatedBefore);

    /**
     * The deployment the gateways run for an API, read as typed objects.
     *
     * <p>{@code events_latest} holds exactly one row per API, so the gateways run the API if and only if that row
     * deployed or started it: a stopped or unpublished API has a row of another type and answers nothing here. The
     * (de)serialization of the payload belongs to this port, so a caller reads the deployment as an {@link Api} and
     * never as JSON.
     *
     * <p>The question is asked for one environment: a deployment of an API of the same id in another one belongs to
     * another tenant, and answering it would let a caller republish it into its own environment.
     *
     * @return empty when the gateways run nothing for this API of this environment — it was never deployed, its last
     *     event stopped or unpublished it, or that event carries a blank payload. The last case leaves a caller
     *     writing the definition and sending the gateways nothing, as a stopped API does: a deployment with nothing
     *     in it is no more republishable than no deployment at all.
     * @throws TechnicalDomainException when the payload or the definition it carries cannot be read. A payload that
     *     is there and unreadable is a failure, unlike one that is not there at all. The deployment is answered whole
     *     or not at all: a caller republishing it would otherwise hand the gateways a definition short of what could
     *     not be parsed.
     */
    Optional<ApiDeployment> findRunningApiDeployment(String environmentId, String apiId);

    /**
     * A deployment of an API as the gateways run it.
     *
     * @param eventId the id of the event that deployed it, so a newer deployment can be told from it.
     * @param type the type of that event, republished as it is: a started API is not restarted by a new deployment of
     *     its definition, and a deployed one is not started by it.
     * @param deploymentNumber the number of that deployment, or {@code null} when it carries none; a republished
     *     event takes the one after it, since the gateways ignore an event of the number they already run.
     * @param api the API the deployment carries, which is what the gateways run — not what the store holds.
     */
    record ApiDeployment(String eventId, EventType type, String deploymentNumber, Api api) {}
}
