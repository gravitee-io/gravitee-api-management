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
package io.gravitee.rest.api.service.builder;

import io.gravitee.rest.api.service.EmailNotification;

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
        this.emailNotification.setTemplate(emailTemplate.getTemplate());
        return this;
    }

    public EmailNotificationBuilder subject(String subject) {
        this.emailNotification.setSubject(subject);
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

    public EmailNotificationBuilder bcc(String[] bcc) {
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

        REVOKE_API_KEY("apiKeyRevoked.html"),
        RENEWED_API_KEY("apiKeyRenewed.html"),
        EXPIRE_API_KEY("apiKeyExpired.html"),
        NEW_SUBSCRIPTION("subscriptionReceived.html"),
        SUBSCRIPTION_CREATED("subscriptionCreated.html"),
        APPROVE_SUBSCRIPTION("subscriptionApproved.html"),
        CLOSE_SUBSCRIPTION("subscriptionClosed.html"),
        PAUSE_SUBSCRIPTION("subscriptionPaused.html"),
        RESUME_SUBSCRIPTION("subscriptionResumed.html"),
        REJECT_SUBSCRIPTION("subscriptionRejected.html"),
        TRANSFER_SUBSCRIPTION("subscriptionTransferred.html"),
        USER_REGISTRATION("userRegistration.html"),
        USER_REGISTERED("userRegistered.html"),
        USER_REGISTRATION_REQUEST("userRegistrationRequest.html"),
        USER_REGISTRATION_REQUEST_PROCESSED("userRegistrationRequestProcessed.html"),
        USER_CREATED("userCreated.html"),
        APPLICATION_MEMBER_SUBSCRIPTION("applicationMember.html"),
        API_MEMBER_SUBSCRIPTION("apiMember.html"),
        GROUP_MEMBER_SUBSCRIPTION("groupMember.html"),
        SUPPORT_TICKET("supportTicket.html"),
        PASSWORD_RESET("passwordReset.html"),
        USER_FIRST_LOGIN("userFirstLogin.html"),
        SUPPORT_TICKET_NOTIFICATION("supportTicketNotification.html"),
        API_STARTED("apiStarted.html"),
        API_STOPPED("apiStopped.html"),
        NEW_RATING("newRating.html"),
        NEW_RATING_ANSWER("newRatingAnswer.html"),
        GENERIC_MESSAGE("genericMessage.html"),
        GROUP_INVITATION("groupInvitation.html"),
        ASK_FOR_REVIEW("askForReview.html"),
        REQUEST_FOR_CHANGES("requestForChanges.html"),
        REVIEW_OK("reviewOk.html"),
        API_DEPRECATED("apiDeprecated.html");

        private String template;

        EmailTemplate(String template) {
            this.template = template;
        }

        public String getTemplate() {
            return template;
        }
    }
}
