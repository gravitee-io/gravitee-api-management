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
package io.gravitee.apim.infra.crud_service.cockpit;

import io.gravitee.apim.core.cockpit.crud_service.InstallationCrudService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InstallationRepository;
import io.gravitee.repository.management.model.Installation;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class InstallationCrudServiceImpl implements InstallationCrudService {

    private final InstallationRepository installationRepository;

    public InstallationCrudServiceImpl(@Lazy InstallationRepository installationRepository) {
        this.installationRepository = installationRepository;
    }

    @Override
    public Optional<String> getInstallationId() {
        try {
            return installationRepository.find().map(Installation::getId);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Impossible to find installation id", e);
        }
    }
}
