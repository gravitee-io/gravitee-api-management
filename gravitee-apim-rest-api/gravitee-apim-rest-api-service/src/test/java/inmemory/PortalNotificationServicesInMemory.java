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
package inmemory;

import io.gravitee.apim.core.portal_notification.crud_service.PortalNotificationCrudService;
import io.gravitee.apim.core.portal_notification.model.PortalNotification;
import io.gravitee.apim.core.portal_notification.model.PortalNotificationQuery;
import io.gravitee.apim.core.portal_notification.model.PortalNotificationReferenceIds;
import io.gravitee.apim.core.portal_notification.query_service.PortalNotificationQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Both ports share one storage, so a notification marked as read is seen as read by the search, as with the real
 * repository.
 */
public class PortalNotificationServicesInMemory
    implements PortalNotificationQueryService, PortalNotificationCrudService, InMemoryAlternative<PortalNotification> {

    private final ArrayList<PortalNotification> storage = new ArrayList<>();

    @Override
    public Page<PortalNotification> search(PortalNotificationQuery criteria, Pageable pageable) {
        var matching = inScope(criteria.user(), criteria.environmentId())
            .filter(n -> criteria.read() == null || criteria.read() == n.isRead())
            .filter(n -> criteria.apiId() == null || criteria.apiId().equals(n.getApiId()))
            .filter(n -> criteria.applicationId() == null || criteria.applicationId().equals(n.getApplicationId()))
            // Mirrors the ORDER BY created_at DESC done at the repository layer.
            .sorted(Comparator.comparing(PortalNotification::getCreatedAt).reversed())
            .toList();
        var content = matching
            .stream()
            .skip((long) (pageable.getPageNumber() - 1) * pageable.getPageSize())
            .limit(pageable.getPageSize())
            .toList();
        return new Page<>(content, pageable.getPageNumber(), content.size(), matching.size());
    }

    @Override
    public PortalNotificationReferenceIds findReferenceIds(String user, String environmentId) {
        return new PortalNotificationReferenceIds(
            distinct(user, environmentId, PortalNotification::getApiId),
            distinct(user, environmentId, PortalNotification::getApplicationId)
        );
    }

    @Override
    public boolean markAsRead(String user, String environmentId, String id, Date readAt) {
        var found = inScope(user, environmentId)
            .filter(n -> n.getId().equals(id))
            .toList();
        found
            .stream()
            .filter(n -> !n.isRead())
            .forEach(n -> n.setReadAt(readAt));
        return !found.isEmpty();
    }

    @Override
    public void markAllAsRead(String user, String environmentId, Date readAt) {
        inScope(user, environmentId)
            .filter(n -> !n.isRead())
            .forEach(n -> n.setReadAt(readAt));
    }

    @Override
    public boolean delete(String user, String environmentId, String id) {
        var toDelete = inScope(user, environmentId)
            .filter(n -> n.getId().equals(id))
            .toList();
        return storage.removeAll(toDelete);
    }

    @Override
    public void deleteAll(String user, String environmentId) {
        storage.removeAll(inScope(user, environmentId).toList());
    }

    private Stream<PortalNotification> inScope(String user, String environmentId) {
        return storage
            .stream()
            .filter(n -> user.equals(n.getUser()))
            .filter(n -> n.getEnvironmentId() == null || n.getEnvironmentId().equals(environmentId));
    }

    private Set<String> distinct(String user, String environmentId, Function<PortalNotification, String> field) {
        return inScope(user, environmentId).map(field).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Override
    public void initWith(List<PortalNotification> items) {
        storage.clear();
        items
            .stream()
            .map(item -> item.toBuilder().build())
            .forEach(storage::add);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<PortalNotification> storage() {
        return Collections.unmodifiableList(storage);
    }
}
