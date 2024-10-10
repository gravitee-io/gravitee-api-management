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
package io.gravitee.rest.api.service.builder;

import io.gravitee.rest.api.service.EmailNotification;
import io.gravitee.rest.api.service.notification.ActionHook;
import io.gravitee.rest.api.service.notification.AlertHook;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notification.Hook;
import io.gravitee.rest.api.service.notification.PortalHook;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EmailNotificationBuilder {

    private final EmailNotification emailNotification = new EmailNotification();

    public EmailNotificationBuilder from(String from) {
        this.emailNotification.setFrom(from);
        return this;
    }

    public EmailNotificationBuilder fromName(String fromName) {
        this.emailNotification.setFromName(fromName);
        return this;
    }

    public EmailNotificationBuilder to(String... to) {
        this.emailNotification.setTo(to);
        return this;
    }

    public EmailNotificationBuilder template(EmailTemplate emailTemplate) {
        this.emailNotification.setTemplate(emailTemplate.getLinkedHook().getTemplate());
        return this;
    }

    public EmailNotificationBuilder param(String key, Object value) {
        if (this.emailNotification.getParams() == null) {
            this.emailNotification.setParams(new HashMap<>());
        }
        this.emailNotification.getParams().put(key, value);
        return this;
    }

    public EmailNotificationBuilder params(Map<String, Object> params) {
        this.emailNotification.setParams(params);
        return this;
    }

    public EmailNotificationBuilder copyToSender(boolean copyToSender) {
        this.emailNotification.setCopyToSender(copyToSender);
        return this;
    }

    public EmailNotificationBuilder bcc(String... bcc) {
        this.emailNotification.setBcc(bcc);
        return this;
    }

    public EmailNotificationBuilder replyTo(String replyTo) {
        this.emailNotification.setReplyTo(replyTo);
        return this;
    }

    public EmailNotification build() {
        return this.emailNotification;
    }

    public enum EmailTemplate {
        API_APIKEY_REVOKED(ApiHook.APIKEY_REVOKED, "apiKeyRevoked.html", "API Key revoked for API ${api.name}"),
        API_APIKEY_RENEWED(ApiHook.APIKEY_RENEWED, "apiKeyRenewed.html", "API Key renewed"),
        API_APIKEY_EXPIRED(ApiHook.APIKEY_EXPIRED, "apiKeyExpired.html", "API Key pollInterval!"),
        API_SUBSCRIPTION_NEW(
            ApiHook.SUBSCRIPTION_NEW,
            "subscriptionReceived.html",
            "New subscription for ${api.name} with plan ${plan.name}"
        ),
        API_SUBSCRIPTION_ACCEPTED(ApiHook.SUBSCRIPTION_ACCEPTED, "subscriptionApproved.html", "Subscription approved"),
        API_SUBSCRIPTION_CLOSED(ApiHook.SUBSCRIPTION_CLOSED, "subscriptionClosed.html", "Subscription closed"),
        API_SUBSCRIPTION_PAUSED(
            ApiHook.SUBSCRIPTION_PAUSED,
            "subscriptionPaused.html",
            "Subscription for ${api.name} with plan ${plan.name} has been paused"
        ),
        API_SUBSCRIPTION_RESUMED(
            ApiHook.SUBSCRIPTION_RESUMED,
            "subscriptionResumed.html",
            "Subscription for ${api.name} with plan ${plan.name} has been resumed"
        ),
        API_SUBSCRIPTION_REJECTED(ApiHook.SUBSCRIPTION_REJECTED, "subscriptionRejected.html", "Subscription rejected"),
        API_SUBSCRIPTION_TRANSFERRED(
            ApiHook.SUBSCRIPTION_TRANSFERRED,
            "subscriptionTransferred.html",
            "Subscription for ${api.name} with plan ${plan.name} has been transferred"
        ),
        API_NEW_SUPPORT_TICKET(ApiHook.NEW_SUPPORT_TICKET, "supportTicketNotification.html", "New Support Ticket"),
        API_API_STARTED(ApiHook.API_STARTED, "apiStarted.html", "API Started"),
        API_API_STOPPED(ApiHook.API_STOPPED, "apiStopped.html", "API Stopped"),
        API_API_UPDATED(ApiHook.API_UPDATED, "apiUpdated.html", "API Updated"),
        API_API_DEPLOYED(ApiHook.API_DEPLOYED, "apiDeployed.html", "API Deployed"),
        API_NEW_RATING(ApiHook.NEW_RATING, "newRating.html", "New Rating"),
        API_NEW_RATING_ANSWER(ApiHook.NEW_RATING_ANSWER, "newRatingAnswer.html", "New Rating Answer"),
        API_ASK_FOR_REVIEW(ApiHook.ASK_FOR_REVIEW, "askForReview.html", "Review asked"),
        API_REQUEST_FOR_CHANGES(ApiHook.REQUEST_FOR_CHANGES, "requestForChanges.html", "Request for changes on API"),
        API_REVIEW_OK(ApiHook.REVIEW_OK, "reviewOk.html", "API review accepted"),
        API_API_DEPRECATED(ApiHook.API_DEPRECATED, "apiDeprecated.html", "API deprecated"),
        API_PLANS_DATA_FIXED(ApiHook.MESSAGE, "apiPlansDataFixed.html", "API plans data have been fixed"),
        APPLICATION_SUBSCRIPTION_NEW(
            ApplicationHook.SUBSCRIPTION_NEW,
            "subscriptionCreated.html",
            "New subscription to ${api.name} with plan ${plan.name}"
        ),
        APPLICATION_SUBSCRIPTION_ACCEPTED(
            ApplicationHook.SUBSCRIPTION_ACCEPTED,
            "subscriptionApproved.html",
            "Your subscription to ${api.name} with plan ${plan.name} has been approved"
        ),
        APPLICATION_SUBSCRIPTION_CLOSED(
            ApplicationHook.SUBSCRIPTION_CLOSED,
            "subscriptionClosed.html",
            "Your subscription to ${api.name} with plan ${plan.name} has been closed"
        ),
        APPLICATION_SUBSCRIPTION_PAUSED(
            ApplicationHook.SUBSCRIPTION_PAUSED,
            "subscriptionPaused.html",
            "Your subscription to ${api.name} with plan ${plan.name} has been paused"
        ),
        APPLICATION_SUBSCRIPTION_RESUMED(
            ApplicationHook.SUBSCRIPTION_RESUMED,
            "subscriptionResumed.html",
            "Your subscription to ${api.name} with plan ${plan.name} has been resumed"
        ),
        APPLICATION_SUBSCRIPTION_REJECTED(
            ApplicationHook.SUBSCRIPTION_REJECTED,
            "subscriptionRejected.html",
            "Your subscription to ${api.name} with plan ${plan.name} has been rejected"
        ),
        APPLICATION_SUBSCRIPTION_TRANSFERRED(
            ApplicationHook.SUBSCRIPTION_TRANSFERRED,
            "subscriptionTransferred.html",
            "Your subscription to ${api.name} with plan ${plan.name} has been transferred"
        ),
        APPLICATION_NEW_SUPPORT_TICKET(
            ApplicationHook.NEW_SUPPORT_TICKET,
            "supportTicketNotification.html",
            "New support Ticket by ${user.displayName}"
        ),
        PORTAL_USER_REGISTRATION_REQUEST(
            PortalHook.USER_REGISTRATION_REQUEST,
            "userRegistrationRequest.html",
            "User registration requested - ${user.displayName}"
        ),
        PORTAL_USER_REGISTERED(PortalHook.USER_REGISTERED, "userRegistered.html", "User registered - ${user.displayName}"),
        PORTAL_USER_CREATED(PortalHook.USER_CREATED, "userCreated.html", "User creation - ${user.displayName}"),
        PORTAL_USER_FIRST_LOGIN(PortalHook.USER_FIRST_LOGIN, "userFirstLogin.html", "First login - ${user.displayName}"),
        PORTAL_PASSWORD_RESET(PortalHook.PASSWORD_RESET, "passwordResetNotification.html", "Password reset - ${user.displayName}"),
        PORTAL_NEW_SUPPORT_TICKET(
            PortalHook.NEW_SUPPORT_TICKET,
            "supportTicketNotification.html",
            "New support Ticket by ${user.displayName}"
        ),
        PORTAL_GROUP_INVITATION(PortalHook.GROUP_INVITATION, "groupInvitationNotification.html", "New group invitation - ${group.name}"),
        PORTAL_FEDERATED_APIS_INGESTION_COMPLETE(
            PortalHook.FEDERATED_APIS_INGESTION_COMPLETE,
            "federatedApisIngestionComplete.html",
            "Federated APIs ingestion complete for Integration ${integration.name}"
        ),
        TEMPLATES_FOR_ACTION_USER_REGISTRATION(
            ActionHook.USER_REGISTRATION,
            "userRegistration.html",
            "User ${registrationAction} - ${user.displayName}"
        ),
        TEMPLATES_FOR_ACTION_USER_REGISTRATION_REQUEST_PROCESSED(
            ActionHook.USER_REGISTRATION_REQUEST_PROCESSED,
            "userRegistrationRequestProcessed.html",
            "User registration ${registrationStatus} - ${user.displayName}"
        ),
        TEMPLATES_FOR_ACTION_APPLICATION_MEMBER_SUBSCRIPTION(
            ActionHook.APPLICATION_MEMBER_SUBSCRIPTION,
            "applicationMember.html",
            "New member in application ${application.name}"
        ),
        TEMPLATES_FOR_ACTION_API_MEMBER_SUBSCRIPTION(ActionHook.API_MEMBER_SUBSCRIPTION, "apiMember.html", "New member in API ${api.name}"),
        TEMPLATES_FOR_ACTION_GROUP_MEMBER_SUBSCRIPTION(
            ActionHook.GROUP_MEMBER_SUBSCRIPTION,
            "groupMember.html",
            "New member in group ${group.name}"
        ),
        TEMPLATES_FOR_ACTION_SUPPORT_TICKET(ActionHook.SUPPORT_TICKET, "supportTicket.html", "${ticketSubject}"),
        TEMPLATES_FOR_ACTION_USER_GROUP_INVITATION(
            ActionHook.USER_GROUP_INVITATION,
            "groupInvitation.html",
            "Group invitation - ${group.name}"
        ),
        TEMPLATES_FOR_ACTION_USER_PASSWORD_RESET(
            ActionHook.USER_PASSWORD_RESET,
            "passwordReset.html",
            "Password reset - ${user.displayName}"
        ),
        TEMPLATES_FOR_ACTION_SUBSCRIPTION_PRE_EXPIRATION(
            ActionHook.SUBSCRIPTION_PRE_EXPIRATION,
            "subscriptionPreExpirationNotification.html",
            "<#if apiKey??>API Key of<#else>Subscription to</#if> ${api.name} will expire in ${expirationDelay} days!"
        ),
        TEMPLATES_FOR_ACTION_GENERIC_MESSAGE(ActionHook.GENERIC_MESSAGE, "genericMessage.html", "${messageSubject}"),
        TEMPLATES_FOR_ALERT_CONSUMER_HTTP_STATUS(
            AlertHook.CONSUMER_HTTP_STATUS,
            "alert_HTTPStatus.html",
            "Alert reached for the application ${application.name}"
        ),
        TEMPLATES_FOR_ALERT_CONSUMER_RESPONSE_TIME(
            AlertHook.CONSUMER_RESPONSE_TIME,
            "alert_responseTime.html",
            "Alert reached for the application ${application.name}"
        );

        private Hook linkedHook;
        private String htmlTemplate;
        private String subject;

        EmailTemplate(Hook linkedHook, String htmlTemplate, String subject) {
            this.linkedHook = linkedHook;
            this.htmlTemplate = htmlTemplate;
            this.subject = subject;
        }

        public String getHtmlTemplate() {
            return htmlTemplate;
        }

        public String getSubject() {
            return subject;
        }

        public Hook getLinkedHook() {
            return linkedHook;
        }

        public static EmailTemplate fromHook(Hook hook) {
            final EmailTemplate[] values = values();
            for (int i = 0; i < values.length; i++) {
                if (values[i].getLinkedHook().equals(hook)) {
                    return values[i];
                }
            }
            return null;
        }

        public static EmailTemplate fromHtmlTemplateName(String htmlTemplate) {
            final EmailTemplate[] values = values();
            for (int i = 0; i < values.length; i++) {
                if (values[i].getHtmlTemplate().equals(htmlTemplate)) {
                    return values[i];
                }
            }
            return null;
        }
    }
}
