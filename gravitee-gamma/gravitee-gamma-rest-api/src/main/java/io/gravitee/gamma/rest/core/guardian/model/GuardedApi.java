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
import io.gravitee.gamma.rest.core.guardian.exception.ApiCannotRunGuardiansException;
import lombok.Builder;

/**
 * An API as the Guardians domain reads it: what identifies it, and the internal flow its Guardians run in.
 *
 * @param supportsGuardians whether the definition of the API holds an internal flow to run Guardians in, which a v4
 *     API does and a native or an edge one does not.
 * @author GraviteeSource Team
 */
@Builder(toBuilder = true)
public record GuardedApi(String id, String environmentId, String name, ApiType type, boolean supportsGuardians, Flow internalFlow) {
    /**
     * @throws ApiCannotRunGuardiansException when the API cannot run Guardians at all — asking which ones it runs is
     *     then a mistake, not an empty answer, and no caller has to remember to check {@link #supportsGuardians()}.
     */
    public ApiGuardians guardians() {
        if (!supportsGuardians) {
            throw new ApiCannotRunGuardiansException(id);
        }
        return ApiGuardians.of(internalFlow);
    }
}
