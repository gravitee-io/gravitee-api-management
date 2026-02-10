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

import io.gravitee.apim.core.subscription_form.crud_service.SubscriptionFormCrudService;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

/**
 * In-memory implementation of SubscriptionFormCrudService for testing.
 *
 * @author Gravitee.io Team
 */
public class SubscriptionFormCrudServiceInMemory implements SubscriptionFormCrudService, InMemoryAlternative<SubscriptionForm> {

    final List<SubscriptionForm> storage = new ArrayList<>();

    @Override
    public SubscriptionForm create(SubscriptionForm subscriptionForm) {
        SubscriptionForm toStore = subscriptionForm.getId() == null
            ? SubscriptionForm.builder()
                .id(SubscriptionFormId.random())
                .environmentId(subscriptionForm.getEnvironmentId())
                .gmdContent(subscriptionForm.getGmdContent())
                .enabled(subscriptionForm.isEnabled())
                .build()
            : subscriptionForm;
        storage.add(toStore);
        return toStore;
    }

    @Override
    public SubscriptionForm update(SubscriptionForm subscriptionForm) {
        OptionalInt index = this.findIndex(this.storage, form -> form.getId().equals(subscriptionForm.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), subscriptionForm);
            return subscriptionForm;
        }

        throw new IllegalStateException("Subscription form not found: " + subscriptionForm.getId());
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
