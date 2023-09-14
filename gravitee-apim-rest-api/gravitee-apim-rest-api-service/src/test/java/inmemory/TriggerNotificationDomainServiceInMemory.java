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
import io.gravitee.apim.core.notification.model.hook.ApiHookContext;
import io.gravitee.apim.core.notification.model.hook.ApplicationHookContext;
import io.gravitee.apim.core.notification.model.hook.HookContext;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TriggerNotificationDomainServiceInMemory implements TriggerNotificationDomainService {

    private final List<ApiHookContext> apiNotifications = new ArrayList<>();
    private final List<ApplicationHookContext> applicationNotifications = new ArrayList<>();
    private final List<ApplicationHookContext> applicationEmailNotifications = new ArrayList<>();

    @Override
    public Map<String, Object> prepareNotificationParameters(ExecutionContext executionContext, HookContext hookContext) {
        return Map.of();
    }

    @Override
    public void triggerApiNotification(ExecutionContext executionContext, ApiHookContext hookContext) {
        apiNotifications.add(hookContext);
    }

    @Override
    public void triggerApiNotification(
        ExecutionContext executionContext,
        ApiHookContext hookContext,
        Map<String, Object> notificationParameters
    ) {
        apiNotifications.add(hookContext);
    }

    @Override
    public void triggerApplicationNotification(ExecutionContext executionContext, ApplicationHookContext hookContext) {
        applicationNotifications.add(hookContext);
    }

    @Override
    public void triggerApplicationNotification(
        ExecutionContext executionContext,
        ApplicationHookContext hookContext,
        Map<String, Object> notificationParameters
    ) {
        applicationNotifications.add(hookContext);
    }

    @Override
    public void triggerApplicationEmailNotification(
        ExecutionContext executionContext,
        ApplicationHookContext hookContext,
        Map<String, Object> notificationParameters,
        String recipient
    ) {
        applicationEmailNotifications.add(hookContext);
    }

    public List<ApiHookContext> getApiNotifications() {
        return Collections.unmodifiableList(apiNotifications);
    }

    public List<ApplicationHookContext> getApplicationNotifications() {
        return Collections.unmodifiableList(applicationNotifications);
    }

    public List<ApplicationHookContext> getApplicationEmailNotifications() {
        return Collections.unmodifiableList(applicationEmailNotifications);
    }

    public void reset() {
        apiNotifications.clear();
        applicationNotifications.clear();
        applicationEmailNotifications.clear();
    }
}
