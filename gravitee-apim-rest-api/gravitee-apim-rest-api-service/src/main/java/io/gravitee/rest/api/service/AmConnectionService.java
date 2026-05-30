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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.AmConnectionEntity;
import java.util.Optional;

/**
 * @author GraviteeSource Team
 */
public interface AmConnectionService {
    Optional<AmConnectionEntity> findByOrganizationId(String organizationId);

    boolean hasToken(String organizationId);

    /**
     * Token semantics: a null token preserves the stored token, a blank token clears it, a non-blank
     * token is encrypted and replaces the stored one.
     * <p>All other fields (baseUrl, defaultDomainId, defaultDomainHrid, gatewayUrl) fully replace the
     * stored values, so a null on any of them clears it.
     */
    AmConnectionEntity save(String organizationId, AmConnectionEntity entity);

    void delete(String organizationId);
}
