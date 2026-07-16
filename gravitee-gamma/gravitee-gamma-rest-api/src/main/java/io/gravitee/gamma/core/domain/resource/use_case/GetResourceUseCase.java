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
package io.gravitee.gamma.core.domain.resource.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.gamma.core.domain.resource.crud_service.ResourceCrudService;
import io.gravitee.gamma.core.domain.resource.exception.ResourceNotFoundException;
import io.gravitee.gamma.core.domain.resource.model.Resource;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetResourceUseCase {

    private final ResourceCrudService resourceCrudService;

    public Output execute(Input input) {
        Resource resource = resourceCrudService
            .findById(input.id())
            .filter(r -> r.referenceId().equals(input.auditInfo().environmentId()))
            .orElseThrow(() -> new ResourceNotFoundException(input.id()));
        return new Output(resource);
    }

    public record Input(AuditInfo auditInfo, String id) {}

    public record Output(Resource resource) {}
}
