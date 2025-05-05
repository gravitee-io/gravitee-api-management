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
package io.gravitee.rest.api.service;

import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PortalNotificationConfigService {
    PortalNotificationConfigEntity save(PortalNotificationConfigEntity notification);
    PortalNotificationConfigEntity findById(String user, NotificationReferenceType referenceType, String referenceId);
    void deleteByUser(String user);
    void deleteReference(NotificationReferenceType referenceType, String referenceId);
    void removeGroupIds(String apiId, Set<String> groupIds);
}
