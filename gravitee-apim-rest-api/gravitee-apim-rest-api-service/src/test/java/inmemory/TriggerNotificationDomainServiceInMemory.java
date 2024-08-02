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

import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.apim.core.notification.model.hook.ApiHookContext;
import io.gravitee.apim.core.notification.model.hook.ApplicationHookContext;
import io.gravitee.apim.core.notification.model.hook.portal.PortalHookContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TriggerNotificationDomainServiceInMemory implements TriggerNotificationDomainService {

    public record ApplicationNotification(Recipient recipient, ApplicationHookContext context) {
        public ApplicationNotification(ApplicationHookContext context) {
            this(new Recipient("DEFAULT", "DEFAULT"), context);
        }
    }

    private final List<ApiHookContext> apiNotifications = new ArrayList<>();
    private final List<ApplicationNotification> applicationNotifications = new ArrayList<>();
    private final List<PortalHookContext> portalNotifications = new ArrayList<>();

    @Override
    public void triggerApiNotification(String organizationId, ApiHookContext hookContext) {
        apiNotifications.add(hookContext);
    }

    @Override
    public void triggerApplicationNotification(String organizationId, ApplicationHookContext hookContext) {
        triggerApplicationNotification(organizationId, hookContext, Collections.emptyList());
    }

    @Override
    public void triggerApplicationNotification(
        String organizationId,
        ApplicationHookContext context,
        List<Recipient> additionalRecipients
    ) {
        applicationNotifications.add(new ApplicationNotification(context));
        additionalRecipients.forEach(recipient -> applicationNotifications.add(new ApplicationNotification(recipient, context)));
    }

    @Override
    public void triggerPortalNotification(String organizationId, PortalHookContext hookContext) {
        portalNotifications.add(hookContext);
    }

    public List<ApiHookContext> getApiNotifications() {
        return Collections.unmodifiableList(apiNotifications);
    }

    public List<ApplicationNotification> getApplicationNotifications() {
        return Collections.unmodifiableList(applicationNotifications);
    }

    public List<PortalHookContext> getPortalNotifications() {
        return Collections.unmodifiableList(portalNotifications);
    }

    public void reset() {
        apiNotifications.clear();
        applicationNotifications.clear();
        portalNotifications.clear();
    }
}
