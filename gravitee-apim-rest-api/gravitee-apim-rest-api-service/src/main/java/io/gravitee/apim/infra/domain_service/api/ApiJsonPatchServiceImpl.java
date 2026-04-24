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
package io.gravitee.apim.infra.domain_service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.gravitee.apim.core.api.domain_service.JsonPatchService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import java.io.IOException;
import org.springframework.stereotype.Service;

/**
 * @author GraviteeSource Team
 */
@Service
public class ApiJsonPatchServiceImpl implements JsonPatchService {

    @Override
    public JsonNode applyJsonPatch(JsonNode patch, JsonNode target) throws ValidationDomainException {
        try {
            return JsonPatch.fromJson(patch).apply(target);
        } catch (JsonPatchException e) {
            throw new ValidationDomainException("Failed to apply patch: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ValidationDomainException("Invalid patch: " + e.getMessage(), e);
        }
    }
}
