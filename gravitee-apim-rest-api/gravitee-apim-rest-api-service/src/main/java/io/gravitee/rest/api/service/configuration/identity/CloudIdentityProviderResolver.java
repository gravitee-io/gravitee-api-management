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
package io.gravitee.rest.api.service.configuration.identity;

import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import java.util.Optional;

/**
 * Resolves the Cloud (Cockpit) identity provider configuration for an organization.
 *
 * The implementation is provided by the Gamma Cloud module plugin when it is deployed;
 * consumers must therefore inject it as optional ({@code @Autowired(required = false)}).
 * When no implementation is present, Cloud identity provider mappings are simply not applied.
 */
public interface CloudIdentityProviderResolver {
    Optional<SocialIdentityProviderEntity> findByOrganizationId(String organizationId);
}
