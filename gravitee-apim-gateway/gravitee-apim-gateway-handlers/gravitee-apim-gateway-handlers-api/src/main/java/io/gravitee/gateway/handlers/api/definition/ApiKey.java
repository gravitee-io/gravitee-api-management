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
package io.gravitee.gateway.handlers.api.definition;

import io.gravitee.repository.management.model.Subscription;
import java.util.ArrayList;
import java.util.List;

/**
 * @author GraviteeSource Team
 */
public class ApiKey extends io.gravitee.repository.management.model.ApiKey {

    private final String plan;
    private final String api;
    private final String subscription;

    public ApiKey(io.gravitee.repository.management.model.ApiKey key, Subscription subscription) {
        super(key);
        this.plan = subscription.getPlan();
        this.api = subscription.getApi();
        this.subscription = subscription.getId();
    }

    @SuppressWarnings("removal")
    public String getPlan() {
        return this.plan;
    }

    @SuppressWarnings("removal")
    public String getApi() {
        return this.api;
    }

    @SuppressWarnings("removal")
    public String getSubscription() {
        return subscription;
    }
}
