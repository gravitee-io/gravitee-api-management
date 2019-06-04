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
package io.gravitee.rest.api.service;

import java.util.List;

import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.api.NewSwaggerApiEntity;
import io.gravitee.rest.api.model.api.UpdateSwaggerApiEntity;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SwaggerService {

    /**
     * Prepare an API from a Swagger descriptor. This method does not create an API but
     * extract data from Swagger to prepare an API to create.
     *
     * @param swaggerDescriptor Swagger descriptor
     * @return The API from the Swagger descriptor
     */
    NewSwaggerApiEntity prepare(ImportSwaggerDescriptorEntity swaggerDescriptor);

    UpdateSwaggerApiEntity prepareForUpdate(ImportSwaggerDescriptorEntity swaggerDescriptor);

    void transform(PageEntity page);

    String replaceServerList(String payload, List<String> graviteeUrls);
}
