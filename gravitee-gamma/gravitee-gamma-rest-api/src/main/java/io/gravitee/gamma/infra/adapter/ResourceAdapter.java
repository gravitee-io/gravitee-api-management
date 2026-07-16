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
package io.gravitee.gamma.infra.adapter;

import io.gravitee.gamma.core.domain.resource.model.Resource;
import java.time.Instant;
import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ResourceAdapter {
    ResourceAdapter INSTANCE = Mappers.getMapper(ResourceAdapter.class);

    default Resource toCoreModel(io.gravitee.repository.management.model.Resource source) {
        if (source == null) {
            return null;
        }
        return Resource.builder()
            .id(source.getId())
            .referenceId(source.getReferenceId())
            .referenceType(toCoreReferenceType(source.getReferenceType()))
            .definition(toDefinition(source))
            .createdAt(dateToInstant(source.getCreatedAt()))
            .updatedAt(dateToInstant(source.getUpdatedAt()))
            .build();
    }

    default io.gravitee.repository.management.model.Resource toRepository(Resource source) {
        if (source == null) {
            return null;
        }
        var builder = io.gravitee.repository.management.model.Resource.builder()
            .id(source.id())
            .referenceId(source.referenceId())
            .referenceType(toRepoReferenceType(source.referenceType()))
            .createdAt(instantToDate(source.createdAt()))
            .updatedAt(instantToDate(source.updatedAt()));

        var definition = source.definition();
        if (definition != null) {
            builder
                .name(definition.getName())
                .type(definition.getType())
                .configuration(definition.getConfiguration())
                .enabled(definition.isEnabled());
        }
        return builder.build();
    }

    default io.gravitee.repository.management.model.Resource.ReferenceType toRepoReferenceType(Resource.ReferenceType source) {
        return source == null ? null : io.gravitee.repository.management.model.Resource.ReferenceType.valueOf(source.name());
    }

    default Resource.ReferenceType toCoreReferenceType(io.gravitee.repository.management.model.Resource.ReferenceType source) {
        return source == null ? null : Resource.ReferenceType.valueOf(source.name());
    }

    default io.gravitee.definition.model.v4.resource.Resource toDefinition(io.gravitee.repository.management.model.Resource source) {
        if (source == null) {
            return null;
        }
        return io.gravitee.definition.model.v4.resource.Resource.builder()
            .name(source.getName())
            .type(source.getType())
            .configuration(source.getConfiguration())
            .enabled(source.isEnabled())
            .build();
    }

    default Date instantToDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    default Instant dateToInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
