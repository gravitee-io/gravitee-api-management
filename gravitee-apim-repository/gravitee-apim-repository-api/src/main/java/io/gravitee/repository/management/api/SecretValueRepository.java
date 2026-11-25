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
import java.util.Optional;

/**
 * Reads the plaintext value of a secret. It stores nothing: a node with a management database decrypts the value from
 * {@link SecretRepository}, and the bridge client reads it from the node that hosts the bridge server.
 */
public interface SecretValueRepository {
    Optional<String> findValue(String environmentId, String key) throws TechnicalException;
}
