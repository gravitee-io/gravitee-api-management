/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service.notifiers.impl;

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.ApiModelEntity;
import io.gravitee.management.model.PlanEntity;
import io.gravitee.management.service.EmailService;
import io.gravitee.management.service.builder.EmailNotificationBuilder;
import io.gravitee.management.service.notification.*;
import io.gravitee.management.service.notifiers.EmailNotifierService;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class EmailNotifierServiceImpl implements EmailNotifierService {

    private final Logger LOGGER = LoggerFactory.getLogger(EmailNotifierServiceImpl.class);

    @Autowired
    EmailService emailService;

    @Override
    public void trigger(final Hook hook, GenericNotificationConfig genericNotificationConfig, final Map<String, Object> params) {
        if (genericNotificationConfig.getConfig() == null || genericNotificationConfig.getConfig().isEmpty()) {
            LOGGER.error("Email Notifier configuration is empty");
            return;
        }
        EmailNotificationBuilder.EmailTemplate emailTemplate = getEmailTemplate(hook);
        if (emailTemplate == null) {
            LOGGER.error("Email template not found for hook {}", hook);
            return;
        }

        String[] mails = genericNotificationConfig.getConfig().split(",|;|\\s");
        for (String mail : mails) {
            if (!mail.isEmpty()) {
                emailService.sendAsyncEmailNotification(new EmailNotificationBuilder()
                        .to(mail)
                        .subject(getEmailSubject(hook, params))
                        .template(emailTemplate)
                        .params(params)
                        .build());
            }
        }
    }

    private EmailNotificationBuilder.EmailTemplate getEmailTemplate(final Hook hook) {
        // Api Hook
        if (hook.equals(ApiHook.APIKEY_REVOKED)) {
            return EmailNotificationBuilder.EmailTemplate.REVOKE_API_KEY;
        }
        else if (hook.equals(ApiHook.APIKEY_EXPIRED)) {
            return EmailNotificationBuilder.EmailTemplate.EXPIRE_API_KEY;
        }
        else if (hook.equals(ApiHook.SUBSCRIPTION_ACCEPTED) || hook.equals(ApiHook.SUBSCRIPTION_NEW)) {
            return EmailNotificationBuilder.EmailTemplate.NEW_SUBSCRIPTION;
        }

        // Application Hook
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_ACCEPTED)) {
            return EmailNotificationBuilder.EmailTemplate.APPROVE_SUBSCRIPTION;
        }
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_REJECTED)) {
            return EmailNotificationBuilder.EmailTemplate.REJECT_SUBSCRIPTION;
        }
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_CLOSED)) {
            return EmailNotificationBuilder.EmailTemplate.CLOSE_SUBSCRIPTION;
        }

        // Portal Hook
        else if (hook.equals(PortalHook.USER_REGISTERED)) {
            return EmailNotificationBuilder.EmailTemplate.USER_REGISTERED;
        }

        // Unknown Hook
        return null;
    }

    private String getEmailSubject(final Hook hook, final Map<String, Object> params) {
        // Api Hook
        if (hook.equals(ApiHook.APIKEY_REVOKED) && params.containsKey(NotificationParamsBuilder.PARAM_API)) {
            Object api = params.get(NotificationParamsBuilder.PARAM_API);
            if (api != null) {
                String apiName = api instanceof ApiModelEntity ? ((ApiModelEntity) api).getName() : ((ApiEntity) api).getName();
                return "API key revoked for API " + apiName;
            }
        }
        else if (hook.equals(ApiHook.APIKEY_EXPIRED)) {
            return "API key expiration!";
        }
        else if (hook.equals(ApiHook.SUBSCRIPTION_ACCEPTED) || hook.equals(ApiHook.SUBSCRIPTION_NEW)) {
            Object api = params.get(NotificationParamsBuilder.PARAM_API);
            Object plan = params.get(NotificationParamsBuilder.PARAM_PLAN);
            if (api != null && plan != null) {
                String apiName = api instanceof ApiModelEntity ? ((ApiModelEntity) api).getName() : ((ApiEntity) api).getName();
                return "New subscription for " + apiName + " with plan " + ((PlanEntity)plan).getName();
            }
        }

        // Application Hook
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_ACCEPTED)) {
            Object api = params.get(NotificationParamsBuilder.PARAM_API);
            Object plan = params.get(NotificationParamsBuilder.PARAM_PLAN);
            if (api != null && plan != null) {
                String apiName = api instanceof ApiModelEntity ? ((ApiModelEntity) api).getName() : ((ApiEntity) api).getName();
                return "Your subscription to " + apiName + " with plan " + ((PlanEntity)plan).getName() + " has been approved";
            }
        }
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_REJECTED)) {
            Object api = params.get(NotificationParamsBuilder.PARAM_API);
            Object plan = params.get(NotificationParamsBuilder.PARAM_PLAN);
            if (api != null && plan != null) {
                String apiName = api instanceof ApiModelEntity ? ((ApiModelEntity) api).getName() : ((ApiEntity) api).getName();
                return "Your subscription to " + apiName + " with plan " + ((PlanEntity)plan).getName() + " has been rejected";
            }
        }
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_CLOSED)) {
            Object api = params.get(NotificationParamsBuilder.PARAM_API);
            Object plan = params.get(NotificationParamsBuilder.PARAM_PLAN);
            if (api != null && plan != null) {
                String apiName = api instanceof ApiModelEntity ? ((ApiModelEntity) api).getName() : ((ApiEntity) api).getName();
                return "Your subscription to " + apiName + " with plan " + ((PlanEntity)plan).getName() + " has been closed";
            }
        }

        // Portal Hook
        else if (hook.equals(PortalHook.USER_REGISTERED)) {
            return "User registration - " + params.get(NotificationParamsBuilder.PARAM_USERNAME);
        }

        // Unknown Hook
        return null;
    }
}
