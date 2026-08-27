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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;

public interface OAIDomainService {
    ImportDefinition convert(String organizationId, String environmentId, ImportSwaggerDescriptorEntity importSwaggerDescriptor);

    /**
     * Returns the OpenAPI document to persist for the imported API.
     *
     * <p>For an inline import the payload already is the document and is kept verbatim, preserving the
     * user's original formatting. For a URL import the payload is only a locator, so the document parsed
     * from it is serialized instead. Note that the parse runs with {@code resolveFully}, so a
     * URL-imported document is stored with its {@code $ref}s inlined.
     */
    String resolveSpecificationContent(String payload);
}
