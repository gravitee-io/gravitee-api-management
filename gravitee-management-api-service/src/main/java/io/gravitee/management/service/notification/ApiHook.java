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
public enum ApiHook implements Hook {

    APIKEY_EXPIRED("API key Expired", "Triggered when an API key is expired.", "API KEY"),
    APIKEY_REVOKED("API key Revoked", "Triggered when an API key is revoked.", "API KEY"),
    SUBSCRIPTION_NEW("New Subscription", "Triggered when a Subscription is created.", "SUBSCRIPTION"),
    SUBSCRIPTION_ACCEPTED("Subscription Accepted", "Triggered when a Subscription is accepted.", "SUBSCRIPTION"),
    SUBSCRIPTION_CLOSED("Subscription Closed", "Triggered when a Subscription is closed.", "SUBSCRIPTION"),
    SUBSCRIPTION_REJECTED("Subscription Rejected", "Triggered when a Subscription is rejected.", "SUBSCRIPTION");

    private String label;
    private String description;
    private String category;

    ApiHook(String label, String description, String category) {
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
        return HookScope.API;
    }


}
