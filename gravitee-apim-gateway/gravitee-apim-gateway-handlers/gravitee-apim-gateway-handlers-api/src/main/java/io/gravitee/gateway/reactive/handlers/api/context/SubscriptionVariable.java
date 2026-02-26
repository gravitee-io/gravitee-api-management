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
package io.gravitee.gateway.reactive.handlers.api.context;

import io.gravitee.gateway.api.service.Subscription;
import java.util.Map;
import lombok.AllArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
public class SubscriptionVariable {

    private final Subscription subscription;

    public String getId() {
        return this.subscription.getId();
    }

    public Subscription.Type getType() {
        return this.subscription.getType();
    }

    public Map<String, String> getMetadata() {
        return this.subscription.getMetadata();
    }

    public String getApplicationName() {
        return this.subscription.getApplicationName();
    }

    public String getClientId() {
        return this.subscription.getClientId();
    }

    public String getApiProductId() {
        return this.subscription.getApiProductId();
    }
}
