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
package io.gravitee.apim.infra.secret;

import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.secret.crud_service.SecretCrudService;
import io.gravitee.apim.core.secret.exception.SecretNotFoundException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.service_provider.SecretValueEncryptor;
import io.gravitee.repository.management.api.SecretValueRepository;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SecretValueRepositoryImpl implements SecretValueRepository {

    private final EnvironmentCrudService environmentCrudService;
    private final SecretCrudService secretCrudService;
    private final SecretValueEncryptor secretValueEncryptor;

    public SecretValueRepositoryImpl(
        EnvironmentCrudService environmentCrudService,
        SecretCrudService secretCrudService,
        SecretValueEncryptor secretValueEncryptor
    ) {
        this.environmentCrudService = environmentCrudService;
        this.secretCrudService = secretCrudService;
        this.secretValueEncryptor = secretValueEncryptor;
    }

    @Override
    public Optional<String> findValue(String environmentId, String key) {
        String organizationId;
        try {
            organizationId = environmentCrudService.get(environmentId).getOrganizationId();
        } catch (EnvironmentNotFoundException e) {
            return Optional.empty();
        }
        return secretCrudService.findByKey(organizationId, environmentId, key).flatMap(this::decryptedValue);
    }

    private Optional<String> decryptedValue(Secret secret) {
        String encryptedValue;
        try {
            encryptedValue = secretCrudService.getEncryptedValue(secret);
        } catch (SecretNotFoundException e) {
            return Optional.empty();
        }
        return Optional.of(secretValueEncryptor.decrypt(secret, encryptedValue));
    }
}
