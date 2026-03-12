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

import io.gravitee.apim.core.basic_auth.crud_service.BasicAuthCredentialsCrudService;
import io.gravitee.apim.core.basic_auth.model.BasicAuthCredentialsEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class BasicAuthCredentialsCrudServiceInMemory
    implements BasicAuthCredentialsCrudService, InMemoryAlternative<BasicAuthCredentialsEntity> {

    final ArrayList<BasicAuthCredentialsEntity> storage = new ArrayList<>();

    @Override
    public BasicAuthCredentialsEntity create(BasicAuthCredentialsEntity credentials) {
        storage.add(credentials);
        return credentials;
    }

    @Override
    public BasicAuthCredentialsEntity update(BasicAuthCredentialsEntity credentials) {
        OptionalInt index = this.findIndex(this.storage, c -> c.getId().equals(credentials.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), credentials);
            return credentials;
        }
        throw new IllegalStateException("BasicAuthCredentials not found");
    }

    @Override
    public Optional<BasicAuthCredentialsEntity> findBySubscriptionId(String subscriptionId) {
        return storage
            .stream()
            .filter(c -> c.getSubscriptions().contains(subscriptionId))
            .findFirst();
    }

    @Override
    public void initWith(List<BasicAuthCredentialsEntity> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<BasicAuthCredentialsEntity> storage() {
        return Collections.unmodifiableList(storage);
    }
}
