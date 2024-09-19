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
package io.gravitee.apim.core.notification.domain_service;

import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.apim.core.notification.model.hook.ApiHookContext;
import io.gravitee.apim.core.notification.model.hook.ApplicationHookContext;
import io.gravitee.apim.core.notification.model.hook.portal.PortalHookContext;
import java.util.List;

public interface TriggerNotificationDomainService {
    void triggerApiNotification(String organizationId, String environmentId, final ApiHookContext context);

    void triggerApplicationNotification(String organizationId, String environmentId, final ApplicationHookContext context);

    void triggerApplicationNotification(
        String organizationId,
        String environmentId,
        final ApplicationHookContext context,
        List<Recipient> additionalRecipients
    );

    void triggerPortalNotification(String organizationId, String environmentId, final PortalHookContext context);
}
