/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.cockpit.services;

import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import java.util.List;

/**
 * @author Julien GIOVARESCO (julien.giovaresco at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ImportSwaggerDescriptorBuilder {

    private ImportSwaggerDescriptorBuilder() {}

    public static ImportSwaggerDescriptorEntity buildForDocumentedApi(String swaggerDefinition) {
        ImportSwaggerDescriptorEntity swaggerDescriptor = new ImportSwaggerDescriptorEntity();
        swaggerDescriptor.setPayload(swaggerDefinition);
        swaggerDescriptor.setWithDocumentation(true);
        swaggerDescriptor.setWithPolicyPaths(true);
        return swaggerDescriptor;
    }

    public static ImportSwaggerDescriptorEntity buildForMockedApi(String swaggerDefinition) {
        ImportSwaggerDescriptorEntity swaggerDescriptor = buildForDocumentedApi(swaggerDefinition);
        swaggerDescriptor.setWithPolicies(List.of("mock"));
        return swaggerDescriptor;
    }
}
