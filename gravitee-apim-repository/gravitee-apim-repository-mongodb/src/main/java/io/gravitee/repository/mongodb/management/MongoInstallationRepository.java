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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InstallationRepository;
import io.gravitee.repository.management.model.Installation;
import io.gravitee.repository.mongodb.management.internal.installation.InstallationMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.InstallationMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class MongoInstallationRepository implements InstallationRepository {

    @Autowired
    private InstallationMongoRepository internalRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Installation> find() throws TechnicalException {
        log.debug("Find installation");
        Installation installation = map(internalRepository.findAll().stream().findFirst().orElse(null));
        log.debug("Find installation - DONE");
        return Optional.ofNullable(installation);
    }

    @Override
    public Optional<Installation> findById(String s) throws TechnicalException {
        log.debug("Find installation by id [{}]", s);
        Installation installation = map(internalRepository.findById(s).orElse(null));
        log.debug("Find installation by id [{}] - DONE", s);
        return Optional.ofNullable(installation);
    }

    @Override
    public Installation create(Installation installation) throws TechnicalException {
        log.debug("Create installation [{}]", installation.getId());
        Installation createdInstallation = map(internalRepository.insert(map(installation)));
        log.debug("Create installation [{}] - Done", createdInstallation.getId());
        return createdInstallation;
    }

    @Override
    public Installation update(Installation installation) throws TechnicalException {
        if (installation == null) {
            throw new IllegalStateException("Installation must not be null");
        }

        final InstallationMongo installationMongo = internalRepository.findById(installation.getId()).orElse(null);
        if (installationMongo == null) {
            throw new IllegalStateException(String.format("No installation found with id [%s]", installation.getId()));
        }

        log.debug("Update installation [{}]", installation.getId());
        Installation updatedInstallation = map(internalRepository.save(map(installation)));
        log.debug("Update installation [{}] - Done", updatedInstallation.getId());
        return updatedInstallation;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete installation [{}]", id);
        internalRepository.deleteById(id);
        log.debug("Delete installation [{}] - Done", id);
    }

    @Override
    public Set<Installation> findAll() throws TechnicalException {
        return internalRepository.findAll().stream().map(this::map).collect(Collectors.toSet());
    }

    private InstallationMongo map(Installation group) {
        return mapper.map(group);
    }

    private Installation map(InstallationMongo installationMongoMongo) {
        return mapper.map(installationMongoMongo);
    }
}
