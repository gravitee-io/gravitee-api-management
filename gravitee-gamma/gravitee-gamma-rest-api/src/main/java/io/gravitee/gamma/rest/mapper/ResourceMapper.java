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
package io.gravitee.gamma.rest.mapper;

import io.gravitee.gamma.core.domain.resource.model.CreateResourceCommand;
import io.gravitee.gamma.core.domain.resource.model.Resource;
import io.gravitee.gamma.core.domain.resource.model.UpdateResourceCommand;
import io.gravitee.gamma.rest.model.CreateResourceRequest;
import io.gravitee.gamma.rest.model.ResourceResponse;
import io.gravitee.gamma.rest.model.UpdateResourceRequest;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ResourceMapper {
    ResourceMapper INSTANCE = Mappers.getMapper(ResourceMapper.class);

    default CreateResourceCommand toCommand(CreateResourceRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateResourceCommand(request.id(), request.name(), request.type(), request.configuration(), request.enabled());
    }

    default UpdateResourceCommand toUpdateCommand(UpdateResourceRequest request) {
        if (request == null) {
            return null;
        }
        return new UpdateResourceCommand(request.name(), request.type(), request.configuration(), request.enabled());
    }

    default ResourceResponse toResponse(Resource resource) {
        if (resource == null) {
            return null;
        }
        return new ResourceResponse(
            resource.id(),
            resource.referenceId(),
            resource.referenceType() == null ? null : resource.referenceType().name(),
            resource.definition(),
            resource.createdAt(),
            resource.updatedAt()
        );
    }

    List<ResourceResponse> toResponseList(List<Resource> resources);
}
