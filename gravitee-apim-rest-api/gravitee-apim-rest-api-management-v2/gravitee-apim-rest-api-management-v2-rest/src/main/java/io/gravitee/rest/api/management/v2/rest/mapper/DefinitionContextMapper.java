/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.mapper;

import static io.gravitee.definition.model.DefinitionContext.*;

import io.gravitee.rest.api.management.v2.rest.model.DefinitionContext;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DefinitionContextMapper {
    DefinitionContextMapper INSTANCE = Mappers.getMapper(DefinitionContextMapper.class);

    default DefinitionContext map(io.gravitee.definition.model.DefinitionContext definitionContext) {
        if (definitionContext == null) {
            return null;
        }
        DefinitionContext context = new DefinitionContext();
        context.setOrigin(
            definitionContext.getOrigin().equals(ORIGIN_MANAGEMENT)
                ? DefinitionContext.OriginEnum.MANAGEMENT
                : DefinitionContext.OriginEnum.KUBERNETES
        );
        context.setMode(
            definitionContext.getMode().equals(MODE_FULLY_MANAGED)
                ? DefinitionContext.ModeEnum.FULLY_MANAGED
                : DefinitionContext.ModeEnum.API_DEFINITION_ONLY
        );
        return context;
    }

    default io.gravitee.definition.model.DefinitionContext map(DefinitionContext definitionContext) {
        if (definitionContext == null || null == definitionContext.getOrigin() || null == definitionContext.getMode()) {
            return null;
        }

        return new io.gravitee.definition.model.DefinitionContext(
            definitionContext.getOrigin() == DefinitionContext.OriginEnum.MANAGEMENT ? ORIGIN_MANAGEMENT : ORIGIN_KUBERNETES,
            definitionContext.getMode() == DefinitionContext.ModeEnum.FULLY_MANAGED ? MODE_FULLY_MANAGED : MODE_API_DEFINITION_ONLY
        );
    }
}
