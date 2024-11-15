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
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum ApiHook implements Hook {
    APIKEY_EXPIRED("API-Key Expired", "Triggered when an API Key is expired.", "API KEY"),
    APIKEY_RENEWED("API-Key Renewed", "Triggered when an API Key is renewed.", "API KEY"),
    APIKEY_REVOKED("API-Key Revoked", "Triggered when an API Key is revoked.", "API KEY"),
    SUBSCRIPTION_NEW("New Subscription", "Triggered when a Subscription is created.", "SUBSCRIPTION"),
    SUBSCRIPTION_ACCEPTED("Subscription Accepted", "Triggered when a Subscription is accepted.", "SUBSCRIPTION"),
    SUBSCRIPTION_CLOSED("Subscription Closed", "Triggered when a Subscription is closed.", "SUBSCRIPTION"),
    SUBSCRIPTION_PAUSED("Subscription Paused", "Triggered when a Subscription is paused.", "SUBSCRIPTION"),
    SUBSCRIPTION_RESUMED("Subscription Resumed", "Triggered when a Subscription is resumed.", "SUBSCRIPTION"),
    SUBSCRIPTION_REJECTED("Subscription Rejected", "Triggered when a Subscription is rejected.", "SUBSCRIPTION"),
    SUBSCRIPTION_TRANSFERRED("Subscription Transferred", "Triggered when a Subscription is transferred.", "SUBSCRIPTION"),
    NEW_SUPPORT_TICKET("New Support Ticket", "Triggered when a new support ticket is created", "SUPPORT"),
    API_STARTED("API Started", "Triggered when an API is started", "LIFECYCLE"),
    API_STOPPED("API Stopped", "Triggered when an API is stopped", "LIFECYCLE"),
    API_UPDATED("API Updated", "Triggered when an API is updated", "LIFECYCLE"),
    API_DEPLOYED("API Deployed", "Triggered when an API is deployed", "LIFECYCLE"),
    NEW_RATING("New Rating", "Triggered when a new rating is submitted", "RATING"),
    NEW_RATING_ANSWER("New Rating Answer", "Triggered when a new answer is submitted", "RATING"),
    MESSAGE("Message", "Used when sending a custom message to an Application Role", null, true),
    ASK_FOR_REVIEW("Ask for API review", "Triggered when an API can be reviewed", "REVIEW"),
    REVIEW_OK("Accept API review", "Triggered when an API's review has been accepted", "REVIEW"),
    REQUEST_FOR_CHANGES("Reject API review", "Triggered when an API's review has been rejected", "REVIEW"),
    API_DEPRECATED("API Deprecated", "Triggered when an API is deprecated", "LIFECYCLE"),
    NEW_SPEC_GENERATED("New specification generated", "Triggered when an API has a new generated specification", "DOCUMENTATION", true);

    private String label;
    private String description;
    private String category;
    private boolean hidden;

    ApiHook(String label, String description, String category) {
        this(label, description, category, false);
    }

    ApiHook(String label, String description, String category, boolean hidden) {
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
        return HookScope.API;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }
}
