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
package io.gravitee.apim.core.portal_notification.query_service;

import io.gravitee.apim.core.portal_notification.model.PortalNotification;
import io.gravitee.apim.core.portal_notification.model.PortalNotificationQuery;
import io.gravitee.apim.core.portal_notification.model.PortalNotificationReferenceIds;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;

public interface PortalNotificationQueryService {
    /**
     * @return the matching notifications, newest first
     */
    Page<PortalNotification> search(PortalNotificationQuery criteria, Pageable pageable);

    /**
     * @return the APIs and applications referenced by the user's notifications of that environment and of no environment
     */
    PortalNotificationReferenceIds findReferenceIds(String user, String environmentId);
}
