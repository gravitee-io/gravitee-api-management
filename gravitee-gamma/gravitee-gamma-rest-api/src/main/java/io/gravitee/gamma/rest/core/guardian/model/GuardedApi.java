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
package io.gravitee.gamma.rest.core.guardian.model;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import lombok.Builder;

/**
 * An API as the Guardians domain reads it: what identifies it, and the internal flow its Guardians run in.
 *
 * @param supportsGuardians whether the API has an internal flow at all, which only a v4 HTTP API does.
 * @author GraviteeSource Team
 */
@Builder(toBuilder = true)
public record GuardedApi(String id, String environmentId, String name, ApiType type, boolean supportsGuardians, Flow internalFlow) {
    public ApiGuardians guardians() {
        return ApiGuardians.of(internalFlow);
    }
}
