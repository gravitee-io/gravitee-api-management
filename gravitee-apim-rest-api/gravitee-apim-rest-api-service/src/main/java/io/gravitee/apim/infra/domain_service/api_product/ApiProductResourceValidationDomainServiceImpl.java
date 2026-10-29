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
package io.gravitee.apim.infra.domain_service.api_product;

import io.gravitee.apim.core.api_product.domain_service.ApiProductResourceValidationDomainService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.service.ResourceService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiProductResourceValidationDomainServiceImpl implements ApiProductResourceValidationDomainService {

    // Plugin ids from each plugin.properties. gravitee-resource-cache-cloud-redis is absent: no product policy uses it.
    static final Set<String> ALLOWED_TYPES = Set.of(
        "cache",
        "cache-redis",
        "oauth2",
        "oauth2-am-resource",
        "oauth2-auth0",
        "oauth2-entra-id",
        "oauth2-keycloak-resource",
        "auth-provider-http-resource",
        "auth-provider-inline-resource",
        "auth-provider-ldap-resource",
        "content-provider-inline-resource"
    );

    private final ResourceService resourceService;

    @Override
    public void validate(List<Resource> resources) {
        if (resources == null || resources.isEmpty()) {
            return;
        }
        Set<String> names = new HashSet<>();
        for (Resource resource : resources) {
            if (resource == null || resource.getName() == null || resource.getName().isBlank()) {
                throw new ValidationDomainException("An API Product resource requires a name");
            }
            if (!names.add(resource.getName())) {
                throw new ValidationDomainException(
                    "Two resources on the same API Product cannot share the name [" + resource.getName() + "]"
                );
            }
            if (resource.getType() == null || !ALLOWED_TYPES.contains(resource.getType())) {
                throw new ValidationDomainException("Resource type [" + resource.getType() + "] is not supported on an API Product");
            }
            if (resource.isEnabled() && (resource.getConfiguration() == null || resource.getConfiguration().isBlank())) {
                throw new ValidationDomainException("Resource [" + resource.getName() + "] requires a configuration");
            }
        }
        for (Resource resource : resources) {
            if (resource.isEnabled()) {
                resourceService.validateResourceConfiguration(resource);
            }
        }
    }
}
