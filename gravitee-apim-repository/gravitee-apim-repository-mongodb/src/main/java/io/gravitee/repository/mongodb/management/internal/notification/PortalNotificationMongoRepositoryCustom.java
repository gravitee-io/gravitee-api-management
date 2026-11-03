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
package io.gravitee.repository.mongodb.management.internal.notification;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.PortalNotificationSearchCriteria;
import io.gravitee.repository.management.model.PortalNotificationReferences;
import io.gravitee.repository.mongodb.management.internal.model.PortalNotificationMongo;
import java.util.Date;

public interface PortalNotificationMongoRepositoryCustom {
    Page<PortalNotificationMongo> search(PortalNotificationSearchCriteria criteria, Pageable pageable);

    PortalNotificationReferences findDistinctReferences(String user, String environmentId);

    boolean markAsRead(String user, String environmentId, String id, Date readAt);

    void markAllAsRead(String user, String environmentId, Date readAt);

    boolean delete(String user, String environmentId, String id);

    void deleteAll(String user, String environmentId);
}
