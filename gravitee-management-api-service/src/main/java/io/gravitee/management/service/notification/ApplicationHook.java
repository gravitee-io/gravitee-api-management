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
package io.gravitee.management.service.notification;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public enum ApplicationHook implements Hook {

    SUBSCRIPTION_NEW("New Subscription", "Triggered when a Subscription is created.", "SUBSCRIPTION"),
    SUBSCRIPTION_ACCEPTED("Subscription Accepted", "Triggered when a Subscription is accepted.", "SUBSCRIPTION"),
    SUBSCRIPTION_CLOSED("Subscription Closed", "Triggered when a Subscription is closed.", "SUBSCRIPTION"),
    SUBSCRIPTION_PAUSED("Subscription Paused", "Triggered when a Subscription is paused.", "SUBSCRIPTION"),
    SUBSCRIPTION_RESUMED("Subscription Resumed", "Triggered when a Subscription is resumed.", "SUBSCRIPTION"),
    SUBSCRIPTION_REJECTED("Subscription Rejected", "Triggered when a Subscription is rejected.", "SUBSCRIPTION"),
    SUBSCRIPTION_TRANSFERRED("Subscription Transferred", "Triggered when a Subscription is transferred.", "SUBSCRIPTION"),
    NEW_SUPPORT_TICKET("New Support Ticket", "Triggered when a new support ticket is created", "SUPPORT");

    private String label;
    private String description;
    private String category;

    ApplicationHook(String label, String description, String category) {
        this.label = label;
        this.description = description;
        this.category = category;
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
        return HookScope.APPLICATION;
    }
}
