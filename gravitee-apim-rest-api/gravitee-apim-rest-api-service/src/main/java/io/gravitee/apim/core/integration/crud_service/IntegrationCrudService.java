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
package io.gravitee.apim.core.integration.crud_service;

import io.gravitee.apim.core.integration.model.Integration;
import java.util.Optional;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface IntegrationCrudService {
    <T extends Integration> T create(T integration);

    Optional<Integration.ApiIntegration> findApiIntegrationById(String id);

    Optional<Integration.A2aIntegration> findA2aIntegrationById(String id);

    Optional<Integration> findById(String id);

    <T extends Integration> T update(T integration);

    void delete(String id);
}
