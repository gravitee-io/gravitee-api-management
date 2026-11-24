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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.gamma.rest.core.guardrail.model.GuardedApi;

/**
 * An APIM API as the Guardrails domain reads it, for every adapter that hands one over.
 *
 * @author GraviteeSource Team
 */
final class GuardedApis {

    private GuardedApis() {}

    static GuardedApi of(Api api) {
        return GuardedApi.builder()
            .id(api.getId())
            .environmentId(api.getEnvironmentId())
            .name(api.getName())
            .type(api.getType())
            .supportsGuardrails(api.holdsInternalFlow())
            .internalFlow(api.internalFlow())
            .build();
    }
}
