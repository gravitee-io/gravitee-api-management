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

import io.gravitee.management.model.*;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.repository.management.model.ApiKey;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class NotificationParamsBuilder {
    private final Map<String, Object> params = new HashMap<>();

    public static final String PARAM_APPLICATION = "application";
    public static final String PARAM_API = "api";
    public static final String PARAM_OWNER = "owner";
    public static final String PARAM_API_KEY = "apiKey";
    public static final String PARAM_PLAN = "plan";
    public static final String PARAM_USER = "user";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_GROUP = "group";
    public static final String PARAM_SUBSCRIPTION_URL = "subscriptionsUrl";
    public static final String PARAM_SUBSCRIPTION = "subscription";
    public static final String PARAM_TOKEN = "token";
    public static final String PARAM_REGISTRATION_URL = "registrationUrl";
    public static final String PARAM_EXPIRATION_DATE = "expirationDate";



    public Map<String, Object> build() {
        return Collections.unmodifiableMap(params);
    }

    public NotificationParamsBuilder application(ApplicationEntity app) {
        this.params.put(PARAM_APPLICATION, app);
        return this;
    }

    public NotificationParamsBuilder plan(PlanEntity plan) {
        this.params.put(PARAM_PLAN, plan);
        return this;
    }

    public NotificationParamsBuilder api(ApiModelEntity api) {
        this.params.put(PARAM_API, api);
        return this;
    }

    public NotificationParamsBuilder api(ApiEntity api) {
        this.params.put(PARAM_API, api);
        return this;
    }

    public NotificationParamsBuilder owner(PrimaryOwnerEntity owner) {
        this.params.put(PARAM_OWNER, owner);
        return this;
    }

    public NotificationParamsBuilder apikey(ApiKey apikey) {
        this.params.put(PARAM_API_KEY, apikey.getKey());
        return this;
    }

    public NotificationParamsBuilder user(UserEntity user) {
        this.params.put(PARAM_USER, user);
        this.params.put(PARAM_USERNAME, user.getUsername());
        return this;
    }

    public NotificationParamsBuilder group(GroupEntity group) {
        this.params.put(PARAM_GROUP, group);
        return this;
    }

    public NotificationParamsBuilder subscriptionsUrl(String subscriptionsUrl) {
        this.params.put(PARAM_SUBSCRIPTION_URL, subscriptionsUrl);
        return this;
    }

    public NotificationParamsBuilder subscription(SubscriptionEntity subscription) {
        this.params.put(PARAM_SUBSCRIPTION, subscription);
        return this;
    }

    public NotificationParamsBuilder token(String token) {
        this.params.put(PARAM_TOKEN, token);
        return this;
    }

    public NotificationParamsBuilder registrationUrl(String registrationUrl) {
        this.params.put(PARAM_REGISTRATION_URL, registrationUrl);
        return this;
    }

    public NotificationParamsBuilder expirationDate(Date expirationDate) {
        this.params.put(PARAM_EXPIRATION_DATE, expirationDate);
        return this;
    }
}
