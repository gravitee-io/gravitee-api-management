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
package io.gravitee.apim.infra.json;

import io.gravitee.apim.core.json.JsonSchemaChecker;
import io.gravitee.rest.api.service.JsonSchemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonSchemaCheckerImpl implements JsonSchemaChecker {

    private final JsonSchemaService jsonSchemaService;

    @Override
    public String validate(String schema, String payload) {
        return jsonSchemaService.validate(schema, payload);
    }
}
