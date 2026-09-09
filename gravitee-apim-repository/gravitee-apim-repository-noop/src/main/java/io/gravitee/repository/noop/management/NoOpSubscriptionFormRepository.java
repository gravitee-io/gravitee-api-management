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
package io.gravitee.repository.noop.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import io.gravitee.repository.management.model.SubscriptionForm;
import java.util.List;
import java.util.Optional;

public class NoOpSubscriptionFormRepository
    extends AbstractNoOpManagementRepository<SubscriptionForm, String>
    implements SubscriptionFormRepository {

    @Override
    public Optional<SubscriptionForm> findByIdAndEnvironmentId(String id, String environmentId) throws TechnicalException {
        return Optional.empty();
    }

    @Override
    public List<SubscriptionForm> findAllByEnvironmentId(String environmentId) throws TechnicalException {
        return List.of();
    }

    @Override
    public Optional<SubscriptionForm> findByEnvironmentIdAndApiId(String environmentId, String apiId) throws TechnicalException {
        return Optional.empty();
    }

    @Override
    public Optional<SubscriptionForm> findDefaultByEnvironmentId(String environmentId) throws TechnicalException {
        return Optional.empty();
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {}
}
