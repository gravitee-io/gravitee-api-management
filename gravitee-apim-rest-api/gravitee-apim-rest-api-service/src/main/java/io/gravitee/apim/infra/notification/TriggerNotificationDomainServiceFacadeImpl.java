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
package io.gravitee.apim.infra.notification;

import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.apim.core.notification.model.hook.ApiHookContext;
import io.gravitee.apim.core.notification.model.hook.ApplicationHookContext;
import io.gravitee.apim.core.notification.model.hook.portal.PortalHookContext;
import io.gravitee.apim.infra.notification.internal.TemplateDataFetcher;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of the TriggerNotificationDomainService interface using APIM "legacy" notification system.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TriggerNotificationDomainServiceFacadeImpl implements TriggerNotificationDomainService {

    private final NotifierService notifierService;
    private final TemplateDataFetcher templateDataFetcher;

    @Override
    public void triggerApiNotification(String organizationId, String environmentId, ApiHookContext context) {
        var notificationParameters = templateDataFetcher.fetchData(organizationId, context);
        notifierService.trigger(
            new ExecutionContext(organizationId, environmentId),
            context.getHook(),
            context.getApiId(),
            notificationParameters
        );
    }

    @Override
    public void triggerApplicationNotification(String organizationId, String environmentId, ApplicationHookContext context) {
        triggerApplicationNotification(organizationId, environmentId, context, Collections.emptyList());
    }

    @Override
    public void triggerApplicationNotification(
        String organizationId,
        String environmentId,
        ApplicationHookContext context,
        List<Recipient> additionalRecipients
    ) {
        var notificationParameters = templateDataFetcher.fetchData(organizationId, context);
        notifierService.trigger(
            new ExecutionContext(organizationId, environmentId),
            context.getHook(),
            context.getApplicationId(),
            notificationParameters,
            additionalRecipients
        );
    }

    @Override
    public void triggerPortalNotification(String organizationId, String environmentId, PortalHookContext context) {
        var notificationParameters = templateDataFetcher.fetchData(organizationId, context);
        notifierService.trigger(new ExecutionContext(organizationId, environmentId), context.getHook(), notificationParameters);
    }
}
