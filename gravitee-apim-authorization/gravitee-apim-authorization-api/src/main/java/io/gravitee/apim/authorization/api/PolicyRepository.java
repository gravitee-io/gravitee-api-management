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
package io.gravitee.apim.authorization.api;

import io.gravitee.apim.authorization.domain.Policy;
import io.gravitee.apim.authorization.domain.PolicyKind;
import java.util.List;
import java.util.Optional;

public interface PolicyRepository {
    Policy save(Policy policy);

    Optional<Policy> findById(String environmentId, String id);

    List<Policy> findAll(String environmentId);

    List<Policy> findByKind(String environmentId, PolicyKind kind);

    List<Policy> findByEntityId(String environmentId, String entityId);

    boolean deleteById(String environmentId, String id);
}
