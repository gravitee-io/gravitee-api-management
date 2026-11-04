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
package io.gravitee.apim.infra.crud_service.guardian;

import io.gravitee.apim.core.guardian.crud_service.GuardianCrudService;
import io.gravitee.apim.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.apim.core.guardian.model.Guardian;
import io.gravitee.apim.infra.adapter.GuardianAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GuardianRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.function.Predicate;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class GuardianCrudServiceImpl implements GuardianCrudService {

    private final GuardianRepository guardianRepository;
    private final GuardianAdapter guardianAdapter;

    public GuardianCrudServiceImpl(@Lazy GuardianRepository guardianRepository, GuardianAdapter guardianAdapter) {
        this.guardianRepository = guardianRepository;
        this.guardianAdapter = guardianAdapter;
    }

    @Override
    public Guardian create(Guardian guardian) {
        try {
            return guardianAdapter.toEntity(guardianRepository.create(guardianAdapter.fromEntity(guardian)));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to create a Guardian with id: %s", guardian.getId()),
                e
            );
        }
    }

    @Override
    public Guardian getByEnvironmentId(String environmentId, String guardianId) {
        try {
            return guardianRepository
                .findById(guardianId)
                .filter(belongsToEnvironment(environmentId))
                .map(guardianAdapter::toEntity)
                .orElseThrow(() -> new GuardianNotFoundException(guardianId));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to find a Guardian with id: %s", guardianId),
                e
            );
        }
    }

    @Override
    public Guardian update(Guardian guardian) {
        try {
            return guardianAdapter.toEntity(guardianRepository.update(guardianAdapter.fromEntity(guardian)));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to update a Guardian with id: %s", guardian.getId()),
                e
            );
        }
    }

    @Override
    public void delete(String guardianId) {
        try {
            guardianRepository.delete(guardianId);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to delete a Guardian with id: %s", guardianId),
                e
            );
        }
    }

    private static Predicate<io.gravitee.repository.management.model.Guardian> belongsToEnvironment(String environmentId) {
        return guardian -> environmentId.equals(guardian.getEnvironmentId());
    }
}
