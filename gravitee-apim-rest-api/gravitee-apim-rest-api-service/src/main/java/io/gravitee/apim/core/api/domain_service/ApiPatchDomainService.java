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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.exception.ValidationDomainException;

/**
 * @author GraviteeSource Team
 */
@DomainService
public class ApiPatchDomainService {

    public JsonNode applyJsonPatch(JsonNode patch, JsonNode target) throws ValidationDomainException {
        try {
            return JsonPatch.fromJson(patch).apply(target);
        } catch (JsonPatchException e) {
            throw new ValidationDomainException("Failed to apply patch: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ValidationDomainException("Invalid patch: " + e.getMessage(), e);
        }
    }

    public JsonNode applyMergePatch(JsonNode patch, JsonNode target) throws ValidationDomainException {
        if (!patch.isObject()) {
            throw new ValidationDomainException("Invalid patch: a merge patch must be a JSON object");
        }
        try {
            return JsonMergePatch.fromJson(patch).apply(target);
        } catch (JsonPatchException e) {
            throw new ValidationDomainException("Failed to apply patch: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ValidationDomainException("Invalid patch: " + e.getMessage(), e);
        }
    }
}
