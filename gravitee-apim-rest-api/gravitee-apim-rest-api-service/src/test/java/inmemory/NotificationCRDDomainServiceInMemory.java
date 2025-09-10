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

import io.gravitee.apim.core.api.domain_service.NotificationCRDDomainService;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import org.springframework.stereotype.Service;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
public class NotificationCRDDomainServiceInMemory implements NotificationCRDDomainService {

    @Override
    public void syncApiPortalNotifications(String apiId, String user, PortalNotificationConfigEntity notificationConfig) {
        // no op
    }
}
