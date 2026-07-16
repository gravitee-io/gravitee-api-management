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
package io.gravitee.gamma.infra.crud_service.resource;

import io.gravitee.gamma.core.domain.resource.crud_service.ResourceCrudService;
import io.gravitee.gamma.core.domain.resource.model.Resource;
import io.gravitee.gamma.infra.adapter.ResourceAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ResourceRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@CustomLog
@Service
public class ResourceCrudServiceImpl extends AbstractService implements ResourceCrudService {

    private final ResourceRepository resourceRepository;

    public ResourceCrudServiceImpl(@Lazy ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Override
    public Resource create(Resource resource) {
        try {
            var created = resourceRepository.create(ResourceAdapter.INSTANCE.toRepository(resource));
            return ResourceAdapter.INSTANCE.toCoreModel(created);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when creating Resource: " + (resource == null ? null : resource.id()), e);
        }
    }

    @Override
    public Resource update(Resource resource) {
        try {
            var updated = resourceRepository.update(ResourceAdapter.INSTANCE.toRepository(resource));
            return ResourceAdapter.INSTANCE.toCoreModel(updated);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when updating Resource: " + (resource == null ? null : resource.id()), e);
        }
    }

    @Override
    public Optional<Resource> findById(String id) {
        try {
            return resourceRepository.findById(id).map(ResourceAdapter.INSTANCE::toCoreModel);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find the resource: " + id, e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            resourceRepository.delete(id);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when deleting Resource: " + id, e);
        }
    }

    @Override
    public boolean existsByNameAndReference(String name, Resource.ReferenceType referenceType, String referenceId) {
        try {
            return resourceRepository.existsByNameAndReference(
                name,
                ResourceAdapter.INSTANCE.toRepoReferenceType(referenceType),
                referenceId
            );
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "Error when checking resource existence by name [" + name + "] and reference [" + referenceType + "/" + referenceId + "]",
                e
            );
        }
    }
}
