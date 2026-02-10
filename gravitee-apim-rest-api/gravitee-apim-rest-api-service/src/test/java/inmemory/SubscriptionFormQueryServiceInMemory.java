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
package inmemory;

import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * In-memory implementation of SubscriptionFormQueryService for testing.
 *
 * @author Gravitee.io Team
 */
public class SubscriptionFormQueryServiceInMemory implements SubscriptionFormQueryService, InMemoryAlternative<SubscriptionForm> {

    final List<SubscriptionForm> storage = new ArrayList<>();

    @Override
    public Optional<SubscriptionForm> findByIdAndEnvironmentId(String environmentId, SubscriptionFormId subscriptionFormId) {
        return storage
            .stream()
            .filter(form -> form.getEnvironmentId().equals(environmentId) && form.getId().equals(subscriptionFormId))
            .findFirst();
    }

    @Override
    public Optional<SubscriptionForm> findDefaultForEnvironmentId(String environmentId) {
        return storage
            .stream()
            .filter(form -> form.getEnvironmentId().equals(environmentId))
            .findFirst();
    }

    @Override
    public void initWith(List<SubscriptionForm> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<SubscriptionForm> storage() {
        return Collections.unmodifiableList(storage);
    }
}
