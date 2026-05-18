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
package io.gravitee.apim.authorization.service;

import io.gravitee.apim.authorization.api.Validators;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CascadeResult(@NotNull List<String> deletedEntityIds, @NotNull List<String> deletedPolicyIds) {
    public CascadeResult {
        Validators.validateCtor(CascadeResult.class, deletedEntityIds, deletedPolicyIds);
        deletedEntityIds = List.copyOf(deletedEntityIds);
        deletedPolicyIds = List.copyOf(deletedPolicyIds);
    }

    public int totalAffected() {
        return deletedEntityIds.size() + deletedPolicyIds.size();
    }
}
