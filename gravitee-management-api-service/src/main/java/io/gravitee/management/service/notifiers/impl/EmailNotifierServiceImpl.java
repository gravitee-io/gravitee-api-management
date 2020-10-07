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

import freemarker.core.InvalidReferenceException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.gravitee.management.model.ApiModelEntity;
import io.gravitee.management.model.PlanEntity;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.service.EmailService;
import io.gravitee.management.service.builder.EmailNotificationBuilder;
import io.gravitee.management.service.notification.*;
import io.gravitee.management.service.notifiers.EmailNotifierService;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.gravitee.management.service.notification.ApiHook.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class EmailNotifierServiceImpl implements EmailNotifierService {

    private final Logger LOGGER = LoggerFactory.getLogger(EmailNotifierServiceImpl.class);

    @Autowired
    EmailService emailService;

    @Autowired
    private Configuration freemarkerConfiguration;

    @Override
    public void trigger(final Hook hook, GenericNotificationConfig genericNotificationConfig, final Map<String, Object> params) {
        if (genericNotificationConfig == null || genericNotificationConfig.getConfig() == null || genericNotificationConfig.getConfig().isEmpty()) {
            LOGGER.error("Email Notifier configuration is empty");
            return;
        }
        EmailNotificationBuilder.EmailTemplate emailTemplate = getEmailTemplate(hook);
        if (emailTemplate == null) {
            LOGGER.error("Email template not found for hook {}", hook);
            return;
        }

        String[] mails = getMails(genericNotificationConfig, params).toArray(new String[0]);
        emailService.sendAsyncEmailNotification(new EmailNotificationBuilder()
                .to(mails)
                .subject(getEmailSubject(hook, params))
                .template(emailTemplate)
                .params(params)
                .build());
    }

    private List<String> getMails(final GenericNotificationConfig genericNotificationConfig, final Map<String, Object> params) {
        String[] mails = genericNotificationConfig.getConfig().split(",|;|\\s");
        List<String> result = new ArrayList<>();
        for (String mail : mails) {
            if (!mail.isEmpty()) {
                if (mail.contains("$")) {
                    try {
                        final Template template = new Template(mail, mail, freemarkerConfiguration);
                        String tmpMail = FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
                        if (!tmpMail.isEmpty()) {
                            result.add(tmpMail);
                        }
                    } catch (IOException | TemplateException e) {
                        LOGGER.error("Email recipient cannot be interpreted {}", mail, e);
                    }
                } else {
                    result.add(mail);
                }
            }
        }
        if (result.isEmpty()) {
            LOGGER.warn("Email recipient not found with: {}", genericNotificationConfig.getConfig());
        }
        return result;
    }

    private EmailNotificationBuilder.EmailTemplate getEmailTemplate(final Hook hook) {
        if (hook == null) {
            return null;
        }
        // Api Hook
        if (hook.equals(ApiHook.APIKEY_REVOKED)) {
            return EmailNotificationBuilder.EmailTemplate.REVOKE_API_KEY;
        }
        else if (hook.equals(ApiHook.APIKEY_RENEWED)) {
            return EmailNotificationBuilder.EmailTemplate.RENEWED_API_KEY;
        }
        else if (hook.equals(ApiHook.APIKEY_EXPIRED)) {
            return EmailNotificationBuilder.EmailTemplate.EXPIRE_API_KEY;
        }
        else if (hook.equals(SUBSCRIPTION_ACCEPTED)) {
            return EmailNotificationBuilder.EmailTemplate.APPROVE_SUBSCRIPTION;
        }
        else if (hook.equals(SUBSCRIPTION_NEW)) {
            return EmailNotificationBuilder.EmailTemplate.NEW_SUBSCRIPTION;
        }
        else if (hook.equals(ApiHook.SUBSCRIPTION_CLOSED)) {
            return EmailNotificationBuilder.EmailTemplate.CLOSE_SUBSCRIPTION;
        }
        else if (hook.equals(ApiHook.SUBSCRIPTION_REJECTED)) {
            return EmailNotificationBuilder.EmailTemplate.REJECT_SUBSCRIPTION;
        }
        else if (hook.equals(SUBSCRIPTION_PAUSED)) {
            return EmailNotificationBuilder.EmailTemplate.PAUSE_SUBSCRIPTION;
        }
        else if (hook.equals(ApiHook.SUBSCRIPTION_RESUMED)) {
            return EmailNotificationBuilder.EmailTemplate.RESUME_SUBSCRIPTION;
        }
        else if (hook.equals(SUBSCRIPTION_TRANSFERRED)) {
            return EmailNotificationBuilder.EmailTemplate.TRANSFER_SUBSCRIPTION;
        }
        else if (hook.equals(ApiHook.NEW_SUPPORT_TICKET)) {
            return EmailNotificationBuilder.EmailTemplate.SUPPORT_TICKET_NOTIFICATION;
        }
        else if (hook.equals(ApiHook.API_STOPPED)) {
            return EmailNotificationBuilder.EmailTemplate.API_STOPPED;
        }
        else if (hook.equals(ApiHook.API_STARTED)) {
            return EmailNotificationBuilder.EmailTemplate.API_STARTED;
        }
        else if (hook.equals(ApiHook.NEW_RATING)) {
            return EmailNotificationBuilder.EmailTemplate.NEW_RATING;
        }
        else if (hook.equals(ApiHook.NEW_RATING_ANSWER)) {
            return EmailNotificationBuilder.EmailTemplate.NEW_RATING_ANSWER;
        }
        else if (hook.equals(ApiHook.ASK_FOR_REVIEW)) {
            return EmailNotificationBuilder.EmailTemplate.ASK_FOR_REVIEW;
        }
        else if (hook.equals(ApiHook.REQUEST_FOR_CHANGES)) {
            return EmailNotificationBuilder.EmailTemplate.REQUEST_FOR_CHANGES;
        }
        else if (hook.equals(ApiHook.REVIEW_OK)) {
            return EmailNotificationBuilder.EmailTemplate.REVIEW_OK;
        }
        else if (hook.equals(API_DEPRECATED)) {
            return EmailNotificationBuilder.EmailTemplate.API_DEPRECATED;
        }

        // Application Hook
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_NEW)) {
            return EmailNotificationBuilder.EmailTemplate.SUBSCRIPTION_CREATED;
        }
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_ACCEPTED)) {
            return EmailNotificationBuilder.EmailTemplate.APPROVE_SUBSCRIPTION;
        }
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_REJECTED)) {
            return EmailNotificationBuilder.EmailTemplate.REJECT_SUBSCRIPTION;
        }
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_CLOSED)) {
            return EmailNotificationBuilder.EmailTemplate.CLOSE_SUBSCRIPTION;
        }
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_PAUSED)) {
            return EmailNotificationBuilder.EmailTemplate.PAUSE_SUBSCRIPTION;
        }
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_RESUMED)) {
            return EmailNotificationBuilder.EmailTemplate.RESUME_SUBSCRIPTION;
        }
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_TRANSFERRED)) {
            return EmailNotificationBuilder.EmailTemplate.TRANSFER_SUBSCRIPTION;
        }
        else if (hook.equals(ApplicationHook.NEW_SUPPORT_TICKET)) {
            return EmailNotificationBuilder.EmailTemplate.SUPPORT_TICKET_NOTIFICATION;
        }

        // Portal Hook
        else if (hook.equals(PortalHook.USER_REGISTERED)) {
            return EmailNotificationBuilder.EmailTemplate.USER_REGISTERED;
        }
        else if (hook.equals(PortalHook.USER_CREATED)) {
            return EmailNotificationBuilder.EmailTemplate.USER_CREATED;
        }
        else if (hook.equals(PortalHook.PASSWORD_RESET)) {
            return EmailNotificationBuilder.EmailTemplate.PASSWORD_RESET;
        }
        else if (hook.equals(PortalHook.USER_FIRST_LOGIN)) {
            return EmailNotificationBuilder.EmailTemplate.USER_FIRST_LOGIN;
        }
        else if (hook.equals(PortalHook.NEW_SUPPORT_TICKET)) {
            return EmailNotificationBuilder.EmailTemplate.SUPPORT_TICKET_NOTIFICATION;
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
        else if (hook.equals(ApiHook.APIKEY_RENEWED)) {
            return "API key renewed";
        }
        else if (hook.equals(ApiHook.SUBSCRIPTION_NEW)) {
            Object api = params.get(NotificationParamsBuilder.PARAM_API);
            Object plan = params.get(NotificationParamsBuilder.PARAM_PLAN);
            if (api != null && plan != null) {
                String apiName = api instanceof ApiModelEntity ? ((ApiModelEntity) api).getName() : ((ApiEntity) api).getName();
                return "New subscription for " + apiName + " with plan " + ((PlanEntity)plan).getName();
            }
        }
        else if (hook.equals(ApiHook.SUBSCRIPTION_ACCEPTED)) {
            return "Subscription approved";
        }
        else if (hook.equals(ApiHook.SUBSCRIPTION_CLOSED)) {
            return "Subscription closed";
        }
        else if (hook.equals(ApiHook.SUBSCRIPTION_REJECTED)) {
            return "Subscription rejected";
        }
        else if (hook.equals(SUBSCRIPTION_PAUSED)) {
            Object api = params.get(NotificationParamsBuilder.PARAM_API);
            Object plan = params.get(NotificationParamsBuilder.PARAM_PLAN);
            if (api != null && plan != null) {
                String apiName = api instanceof ApiModelEntity ? ((ApiModelEntity) api).getName() : ((ApiEntity) api).getName();
                return "Subscription for " + apiName + " with plan " + ((PlanEntity)plan).getName() + " has been paused";
            }
        }
        else if (hook.equals(SUBSCRIPTION_RESUMED)) {
            Object api = params.get(NotificationParamsBuilder.PARAM_API);
            Object plan = params.get(NotificationParamsBuilder.PARAM_PLAN);
            if (api != null && plan != null) {
                String apiName = api instanceof ApiModelEntity ? ((ApiModelEntity) api).getName() : ((ApiEntity) api).getName();
                return "Subscription for " + apiName + " with plan " + ((PlanEntity)plan).getName() + " has been resumed";
            }
        }
        else if (hook.equals(SUBSCRIPTION_TRANSFERRED)) {
            Object api = params.get(NotificationParamsBuilder.PARAM_API);
            Object plan = params.get(NotificationParamsBuilder.PARAM_PLAN);
            if (api != null && plan != null) {
                String apiName = api instanceof ApiModelEntity ? ((ApiModelEntity) api).getName() : ((ApiEntity) api).getName();
                return "Subscription for " + apiName + " with plan " + ((PlanEntity)plan).getName() + " has been transferred";
            }
        }
        else if (hook.equals(ApiHook.NEW_SUPPORT_TICKET)) {
            return "New Support Ticket";
        }
        else if (hook.equals(ApiHook.API_STOPPED)) {
            return "API Stopped";
        }
        else if (hook.equals(ApiHook.API_STARTED)) {
            return "API Started";
        }
        else if (hook.equals(ApiHook.NEW_RATING)) {
            return "New Rating";
        }
        else if (hook.equals(ApiHook.NEW_RATING_ANSWER)) {
            return "New Rating Answer";
        }
        else if (hook.equals(ApiHook.ASK_FOR_REVIEW)) {
            return "Review asked";
        }
        else if (hook.equals(ApiHook.REQUEST_FOR_CHANGES)) {
            return "Request for changes on API";
        }
        else if (hook.equals(ApiHook.REVIEW_OK)) {
            return "API review accepted";
        }
        else if (hook.equals(API_DEPRECATED)) {
            return "API deprecated";
        }

        // Application Hook
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_NEW)) {
            Object api = params.get(NotificationParamsBuilder.PARAM_API);
            Object plan = params.get(NotificationParamsBuilder.PARAM_PLAN);
            if (api != null && plan != null) {
                String apiName = api instanceof ApiModelEntity ? ((ApiModelEntity) api).getName() : ((ApiEntity) api).getName();
                return "New subscription to " + apiName + " with plan " + ((PlanEntity)plan).getName();
            }
        }
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
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_PAUSED)) {
            Object api = params.get(NotificationParamsBuilder.PARAM_API);
            Object plan = params.get(NotificationParamsBuilder.PARAM_PLAN);
            if (api != null && plan != null) {
                String apiName = api instanceof ApiModelEntity ? ((ApiModelEntity) api).getName() : ((ApiEntity) api).getName();
                return "Your subscription to " + apiName + " with plan " + ((PlanEntity)plan).getName() + " has been paused";
            }
        }
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_RESUMED)) {
            Object api = params.get(NotificationParamsBuilder.PARAM_API);
            Object plan = params.get(NotificationParamsBuilder.PARAM_PLAN);
            if (api != null && plan != null) {
                String apiName = api instanceof ApiModelEntity ? ((ApiModelEntity) api).getName() : ((ApiEntity) api).getName();
                return "Your subscription to " + apiName + " with plan " + ((PlanEntity)plan).getName() + " has been resumed";
            }
        }
        else if (hook.equals(ApplicationHook.SUBSCRIPTION_TRANSFERRED)) {
            Object api = params.get(NotificationParamsBuilder.PARAM_API);
            Object plan = params.get(NotificationParamsBuilder.PARAM_PLAN);
            if (api != null && plan != null) {
                String apiName = api instanceof ApiModelEntity ? ((ApiModelEntity) api).getName() : ((ApiEntity) api).getName();
                return "Your subscription to " + apiName + " with plan " + ((PlanEntity)plan).getName() + " has been transferred";
            }
        }
        else if (hook.equals(ApplicationHook.NEW_SUPPORT_TICKET)) {
            return "New Support Ticket by " + params.get(NotificationParamsBuilder.PARAM_USERNAME);
        }

        // Portal Hook
        else if (hook.equals(PortalHook.USER_REGISTERED)) {
            return "User registration - " + params.get(NotificationParamsBuilder.PARAM_USERNAME);
        }
        else if (hook.equals(PortalHook.USER_CREATED)) {
            return "User creation - " + params.get(NotificationParamsBuilder.PARAM_USERNAME);
        }
        else if (hook.equals(PortalHook.PASSWORD_RESET)) {
            return "Password reset - " + params.get(NotificationParamsBuilder.PARAM_USERNAME);
        }
        else if (hook.equals(PortalHook.USER_FIRST_LOGIN)) {
            return "First login - " + params.get(NotificationParamsBuilder.PARAM_USERNAME);
        }
        else if (hook.equals(PortalHook.NEW_SUPPORT_TICKET)) {
            return "New Support Ticket by " + params.get(NotificationParamsBuilder.PARAM_USERNAME);
        }

        // Unknown Hook
        return null;
    }
}
