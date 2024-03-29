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
package io.gravitee.rest.api.spec.converter;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.InputStream;

/**
 *
 * @author GraviteeSource Team
 */
public interface OpenAPIConverter {
    String DEFAULT_API_VERSION = "1.0.0";

    OpenAPI toOpenAPI(InputStream uri);
    OpenAPI toOpenAPI(String content);
}
