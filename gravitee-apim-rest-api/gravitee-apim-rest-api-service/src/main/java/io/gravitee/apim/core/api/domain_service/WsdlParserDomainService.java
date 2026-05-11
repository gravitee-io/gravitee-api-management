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

/**
 * Converts a WSDL descriptor into an OpenAPI YAML string
 * that can be fed into the standard v4 OAI import pipeline.
 */
public interface WsdlParserDomainService {
    /**
     * @param content inline WSDL XML content or a remote URL
     * @return OpenAPI 3.x YAML string
     * @throws io.gravitee.rest.api.service.exceptions.SwaggerDescriptorException if parsing fails
     */
    String toOpenApiYaml(String content);
}
