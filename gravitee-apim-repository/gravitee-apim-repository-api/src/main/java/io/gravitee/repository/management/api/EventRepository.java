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
package io.gravitee.repository.management.api;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Event API.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface EventRepository extends CrudRepository<Event, String> {
    /**
     * Search for {@link Event} with {@link Pageable} feature.
     *
     * <p>
     *  Note that events must be ordered by update date in DESC mode.
     * </p>
     *
     * @param filter Event criteria to search for {@link Event}.
     * @param pageable If user wants a paginable result. Can be <code>null</code>.
     * @return the list of events.
     */
    Page<Event> search(EventCriteria filter, Pageable pageable);
    /**
     * Search for {@link Event}.
     *
     * <p>
     *  Note that events must be ordered by update date in DESC mode.
     * </p>
     * @param filter Event criteria to search for {@link Event}.
     * @return the list of events.
     */
    List<Event> search(EventCriteria filter);

    /**
     * This method allows to create an event if it does not exist in database or patch it if it's present.
     *
     * <p>
     * The patch will do a partial update: <br/>
     * - Root fields of {@link Event} object will be updated only if non-null, i.e it won't removed or update to <code>null</code> any of <code>null</code> fields<br/>
     * - Updated properties map will be used to update existing property or added based on its key, i.e it won't removed or update to <code>null</code> any of <code>null</code> non existing properties.<br/>
     * - Old environments will be removed and replaced only if the new properties map is non-null.
     * </p>
     *
     * createdAt field is not updatable
     * @param event
     * @return the event passed as parameter.
     * @throws TechnicalException
     */
    Event createOrPatch(Event event) throws TechnicalException;

    /**
     * Delete events of a specific API.
     *
     * @param apiId the API id.
     * @return the number of deleted events.
     * @throws TechnicalException
     */
    long deleteApiEvents(String apiId) throws TechnicalException;

    /**
     * Find all events by environmentId
     * @param environmentId
     * @return List of events
     */
    List<Event> findByEnvironmentId(String environmentId);

    /**
     * Find all events by organizationId
     * @param organizationId
     * @return List of events
     */
    List<Event> findByOrganizationId(String organizationId);

    Stream<EventToClean> findEventsToClean(String environmentId);

    void delete(Collection<String> ids);

    record EventToClean(String id, EventToCleanGroup group) {}

    record EventToCleanGroup(String type, String referenceId) {}

    /**
     * Helper class for determining group keys for event cleanup.
     */
    final class EventGroupKeyHelper {

        private EventGroupKeyHelper() {
            // Utility class, prevent instantiation
        }

        /**
         * Creates a structured group for event cleanup that includes both event type and reference ID.
         *
         * @param eventType the event type
         * @param properties the event properties map
         * @return the structured group, or null if the event should not be grouped
         */
        public static EventToCleanGroup determineGroup(EventType eventType, Map<String, String> properties) {
            if (eventType == null || properties == null) {
                return null;
            }

            String referenceId = switch (eventType) {
                case PUBLISH_API, PUBLISH_API_RESULT, UNPUBLISH_API, UNPUBLISH_API_RESULT, START_API, STOP_API, DEBUG_API -> properties.get(
                    Event.EventProperties.API_ID.getValue()
                ); // API events are grouped by API ID
                case PUBLISH_DICTIONARY, UNPUBLISH_DICTIONARY, START_DICTIONARY, STOP_DICTIONARY -> properties.get(
                    // Dictionary events are grouped by dictionary ID
                    Event.EventProperties.DICTIONARY_ID.getValue()
                );
                case PUBLISH_ORGANIZATION, PUBLISH_ORGANIZATION_LICENSE -> properties.get(
                    // Organization events are grouped by organization ID
                    Event.EventProperties.ORGANIZATION_ID.getValue()
                );
                case DEPLOY_SHARED_POLICY_GROUP, UNDEPLOY_SHARED_POLICY_GROUP -> properties.get(
                    // Shared policy group events are grouped by shared policy group ID
                    Event.EventProperties.SHARED_POLICY_GROUP_ID.getValue()
                );
                case GATEWAY_STARTED, GATEWAY_STOPPED -> properties.get(Event.EventProperties.ID.getValue()); // Gateway events are grouped by gateway ID (if available)
                default -> properties.get(Event.EventProperties.ID.getValue()); // For unknown event types, try to use a generic ID property
            };

            return referenceId != null && !referenceId.isBlank() ? new EventToCleanGroup(eventType.name(), referenceId) : null;
        }
    }
}
