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
 * Notification hooks for API Product–scoped events ({@link HookScope#API_PRODUCT}).
 * Use these instead of {@link ApiHook} when the reference is an API Product so subscribers
 * and portal/generic config lookups match the correct {@code HookScope}.
 *
 * @author GraviteeSource Team
 */
public enum ApiProductHook implements Hook {
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
    SUBSCRIPTION_FAILED("Subscription Failed", "Triggered when a Subscription fails.", "SUBSCRIPTION"),
    API_PRODUCT_DEPLOYED("API Product Deployed", "Triggered when an API Product is deployed.", "LIFECYCLE"),
    API_PRODUCT_UPDATED("API Product Updated", "Triggered when an API Product is updated.", "LIFECYCLE");

    private final String label;
    private final String description;
    private final String category;

    ApiProductHook(String label, String description, String category) {
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
        return HookScope.API_PRODUCT;
    }
}
