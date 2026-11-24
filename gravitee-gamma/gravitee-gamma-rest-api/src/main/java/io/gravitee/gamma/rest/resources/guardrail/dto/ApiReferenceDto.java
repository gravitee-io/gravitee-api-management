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
package io.gravitee.gamma.rest.resources.guardrail.dto;

import io.gravitee.gamma.rest.core.guardrail.model.ApiReference;

/**
 * An API named in a Guardrail answer, enough to show it and to link to it.
 *
 * @author GraviteeSource Team
 */
public record ApiReferenceDto(String id, String name) {
    public static ApiReferenceDto from(ApiReference api) {
        return new ApiReferenceDto(api.id(), api.name());
    }
}
