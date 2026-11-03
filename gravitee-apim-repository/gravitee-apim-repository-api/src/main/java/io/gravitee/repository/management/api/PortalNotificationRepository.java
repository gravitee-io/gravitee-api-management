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
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.PortalNotificationSearchCriteria;
import io.gravitee.repository.management.model.PortalNotification;
import io.gravitee.repository.management.model.PortalNotificationReferences;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * The methods taking an {@code environmentId} work on the user's notifications of that environment and on the
 * user's notifications without environment.
 *
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PortalNotificationRepository extends CrudRepository<PortalNotification, String> {
    List<PortalNotification> findByUser(String user) throws TechnicalException;

    /**
     * @return the matching notifications, newest first
     */
    Page<PortalNotification> search(PortalNotificationSearchCriteria criteria, Pageable pageable) throws TechnicalException;

    PortalNotificationReferences findDistinctReferences(String user, String environmentId) throws TechnicalException;

    /**
     * @return {@code false} when no notification with this id belongs to the user in that scope
     */
    boolean markAsRead(String user, String environmentId, String id, Date readAt) throws TechnicalException;

    /**
     * Sets {@code readAt} on the unread notifications only.
     */
    void markAllAsRead(String user, String environmentId, Date readAt) throws TechnicalException;

    void create(List<PortalNotification> notifications) throws TechnicalException;

    /**
     * @return {@code false} when no notification with this id belongs to the user in that scope
     */
    boolean delete(String user, String environmentId, String id) throws TechnicalException;

    /**
     * Deletes the user's notifications of every environment.
     */
    void deleteAll(String user) throws TechnicalException;

    void deleteAll(String user, String environmentId) throws TechnicalException;

    default PortalNotification update(PortalNotification item) throws TechnicalException {
        throw new UnsupportedOperationException("Update a notification is forbidden");
    }

    Optional<PortalNotification> findById(String id) throws TechnicalException;
}
