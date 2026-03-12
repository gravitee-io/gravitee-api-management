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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.BasicAuthCredentialsCriteria;
import io.gravitee.repository.management.model.BasicAuthCredentials;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BasicAuthCredentialsRepository {
    Optional<BasicAuthCredentials> findById(String id) throws TechnicalException;

    Optional<BasicAuthCredentials> findByUsername(String username) throws TechnicalException;

    Optional<BasicAuthCredentials> findByUsernameAndEnvironmentId(String username, String environmentId) throws TechnicalException;

    BasicAuthCredentials create(BasicAuthCredentials credentials) throws TechnicalException;

    BasicAuthCredentials update(BasicAuthCredentials credentials) throws TechnicalException;

    Set<BasicAuthCredentials> findBySubscription(String subscription) throws TechnicalException;

    List<BasicAuthCredentials> findByApplication(String applicationId) throws TechnicalException;

    List<BasicAuthCredentials> findByCriteria(BasicAuthCredentialsCriteria criteria) throws TechnicalException;

    Optional<BasicAuthCredentials> addSubscription(String id, String subscriptionId) throws TechnicalException;

    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
