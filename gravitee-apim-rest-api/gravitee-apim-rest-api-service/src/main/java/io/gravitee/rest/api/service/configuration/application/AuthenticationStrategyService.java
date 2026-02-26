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
package io.gravitee.rest.api.service.configuration.application;

import io.gravitee.rest.api.model.configuration.application.registration.AuthenticationStrategyEntity;
import io.gravitee.rest.api.model.configuration.application.registration.NewAuthenticationStrategyEntity;
import io.gravitee.rest.api.model.configuration.application.registration.UpdateAuthenticationStrategyEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Set;

/**
 * Service for managing authentication strategies.
 * An authentication strategy defines how applications authenticate with APIs,
 * optionally referencing a ClientRegistrationProvider for DCR-based strategies.
 */
public interface AuthenticationStrategyService {

    Set<AuthenticationStrategyEntity> findAll(ExecutionContext executionContext);

    AuthenticationStrategyEntity findById(String environmentId, String id);

    AuthenticationStrategyEntity create(
        ExecutionContext executionContext,
        NewAuthenticationStrategyEntity newStrategy
    );

    AuthenticationStrategyEntity update(
        ExecutionContext executionContext,
        String id,
        UpdateAuthenticationStrategyEntity updateStrategy
    );

    void delete(ExecutionContext executionContext, String id);

    /**
     * Find all strategies that reference a given DCR provider.
     */
    Set<AuthenticationStrategyEntity> findByClientRegistrationProviderId(
        ExecutionContext executionContext,
        String clientRegistrationProviderId
    );
}
