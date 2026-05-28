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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalAdapter {
    PortalAdapter INSTANCE = Mappers.getMapper(PortalAdapter.class);

    Portal toEntity(io.gravitee.repository.management.model.Portal portal);
    io.gravitee.repository.management.model.Portal toRepository(Portal portal);

    default String mapPortalId(PortalId id) {
        return id != null ? id.toString() : null;
    }

    default PortalId mapPortalId(String id) {
        return id != null ? PortalId.of(id) : null;
    }
}
