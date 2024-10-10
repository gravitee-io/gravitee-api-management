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
package io.gravitee.rest.api.service.notification;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum ActionHook implements Hook {
    USER_REGISTRATION(
        "User registration",
        "Email sent to a user who has self-registered on portal or admin console. Contains a registration link.",
        "USER"
    ),
    USER_REGISTRATION_REQUEST_PROCESSED(
        "User registration request processed",
        "Email sent to a user who has self-registered but self-registration need approval. Contains information about request status.",
        "USER"
    ),
    APPLICATION_MEMBER_SUBSCRIPTION("New member of application", "Email sent to user that has been added to an application.", "MEMBERSHIP"),
    API_MEMBER_SUBSCRIPTION("New member of API", "Email sent to user that has been added to an API.", "MEMBERSHIP"),
    GROUP_MEMBER_SUBSCRIPTION("New member of group", "Email sent to user that has been added to a group.", "MEMBERSHIP"),
    SUPPORT_TICKET(
        "Support ticket",
        "Email sent to support team of an API or of the platform, when a support ticket is created.",
        "SUPPORT"
    ),
    GENERIC_MESSAGE("Generic message", "Email sent when using the messaging service.", "SUPPORT", true),
    USER_GROUP_INVITATION("User group invitation", "Email sent when using the messaging service.", "USER"),
    USER_PASSWORD_RESET("User password reset", "Email sent to a user which password has been reset.", "USER"),
    SUBSCRIPTION_PRE_EXPIRATION(
        "Subscription pre-pollInterval notification",
        "Email sent to the subscriber and the primary owner of an application when a the subscription will expire after a specific duration.",
        "SUBSCRIPTION"
    );

    private String label;
    private String description;
    private String category;
    private boolean hidden;

    ActionHook(String label, String description, String category) {
        this(label, description, category, false);
    }

    ActionHook(String label, String description, String category, boolean hidden) {
        this.label = label;
        this.description = description;
        this.category = category;
        this.hidden = hidden;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public HookScope getScope() {
        return HookScope.TEMPLATES_FOR_ACTION;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }
}
