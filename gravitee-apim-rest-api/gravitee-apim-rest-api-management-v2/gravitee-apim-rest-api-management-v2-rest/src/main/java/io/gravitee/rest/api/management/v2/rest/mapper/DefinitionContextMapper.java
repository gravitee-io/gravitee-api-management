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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.rest.api.management.v2.rest.model.DefinitionContext;
import io.gravitee.rest.api.model.context.OriginContext;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DefinitionContextMapper {
    DefinitionContextMapper INSTANCE = Mappers.getMapper(DefinitionContextMapper.class);

    default DefinitionContext map(OriginContext originContext) {
        if (originContext == null) {
            return null;
        }

        if (originContext instanceof OriginContext.Management) {
            return new DefinitionContext()
                .origin(DefinitionContext.OriginEnum.MANAGEMENT)
                .mode(DefinitionContext.ModeEnum.FULLY_MANAGED)
                .syncFrom(DefinitionContext.SyncFromEnum.MANAGEMENT);
        } else if (originContext instanceof OriginContext.Kubernetes) {
            return new DefinitionContext()
                .origin(DefinitionContext.OriginEnum.KUBERNETES)
                .mode(DefinitionContext.ModeEnum.FULLY_MANAGED)
                .syncFrom(DefinitionContext.SyncFromEnum.KUBERNETES);
        }
        return null;
    }
}
