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
package io.gravitee.management.service;

import io.gravitee.management.model.ImportSwaggerDescriptorEntity;
import io.gravitee.management.model.api.NewSwaggerApiEntity;
import io.gravitee.management.service.impl.swagger.transformer.SwaggerTransformer;
import io.gravitee.management.service.swagger.SwaggerDescriptor;

import java.util.Collection;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SwaggerService {

    /**
     * Create an API from a Swagger descriptor. This method does not create an API but
     * extract data from Swagger to prepare an API to then create.
     *
     * @param swaggerDescriptor Swagger descriptor
     * @return The API from the Swagger descriptor
     */
    NewSwaggerApiEntity createAPI(ImportSwaggerDescriptorEntity swaggerDescriptor);

    /**
     * This method is used to transform a Swagger descriptor specification using swagger transformers.
     *
     * @param descriptor
     * @param transformers
     * @param <S>
     * @param <T>
     */
    <S, T extends SwaggerDescriptor<S>> void transform(T descriptor, Collection<SwaggerTransformer<T>> transformers);

    /**
     * This method is used to parse a content (can be a plain text content or an URL starting with http|https|file)
     *
     * @param content
     * @return A swagger descriptor
     */
    SwaggerDescriptor parse(String content);
}
