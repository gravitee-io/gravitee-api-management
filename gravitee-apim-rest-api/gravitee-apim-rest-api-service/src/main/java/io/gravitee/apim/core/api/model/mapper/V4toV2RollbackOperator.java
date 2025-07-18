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
package io.gravitee.apim.core.api.model.mapper;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;

public class V4toV2RollbackOperator {

    public Api rollback(Api source, io.gravitee.definition.model.Api apiDefinition) {
        return source
            .toBuilder()
            .name(apiDefinition.getName())
            .version(apiDefinition.getVersion())
            .definitionVersion(DefinitionVersion.V2)
            .updatedAt(TimeProvider.now())
            .type(ApiType.PROXY)
            .apiDefinition(apiDefinition.toBuilder().plans(null).build())
            .apiDefinitionHttpV4(null)
            .build();
    }

    public Plan rollback(Plan source, io.gravitee.definition.model.Plan planDefinition) {
        return source
            .toBuilder()
            .planDefinitionV2(planDefinition)
            .definitionVersion(io.gravitee.definition.model.DefinitionVersion.V2)
            .updatedAt(TimeProvider.now())
            .planDefinitionHttpV4(null)
            .name(planDefinition.getName())
            .build();
    }
}
