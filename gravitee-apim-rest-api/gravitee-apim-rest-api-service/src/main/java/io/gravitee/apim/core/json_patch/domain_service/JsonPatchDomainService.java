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
package io.gravitee.apim.core.json_patch.domain_service;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import lombok.RequiredArgsConstructor;

/**
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class JsonPatchDomainService {

    private final JsonMergePatchService jsonMergePatchService;
    private final JsonPatchService jsonPatchService;

    public JsonNode applyMergePatch(JsonNode patch, JsonNode target) throws ValidationDomainException {
        if (!patch.isObject()) {
            throw new ValidationDomainException("Invalid patch: a merge patch must be a JSON object");
        }
        return jsonMergePatchService.applyMergePatch(patch, target);
    }

    public JsonNode applyJsonPatch(JsonNode patch, JsonNode target) throws ValidationDomainException {
        if (!patch.isArray()) {
            throw new ValidationDomainException("Invalid patch: a JSON patch must be an array");
        }
        return jsonPatchService.applyJsonPatch(patch, target);
    }
}
