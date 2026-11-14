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
package io.gravitee.gamma.rest.resources.guardian.dto;

import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardian;

/**
 * One Guardian enabled on an API.
 *
 * @param name {@code null} when the Guardian no longer exists in the environment.
 * @param order the 1-based position of the Guardian among those of its phase.
 * @author GraviteeSource Team
 */
public record ApiGuardianDto(String id, String name, Phase phase, int order) {
    public static ApiGuardianDto from(ApiGuardian guardian) {
        return new ApiGuardianDto(guardian.guardianId(), guardian.name(), guardian.phase(), guardian.order());
    }
}
