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
package io.gravitee.rest.api.service.impl;

import io.gravitee.repository.management.api.InstallationRepository;
import io.gravitee.repository.management.model.Installation;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.model.InstallationStatus;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.InstallationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class InstallationServiceImpl implements InstallationService {

    @Value("${cockpit.url:${cloud.url:https://cockpit.gravitee.io}}")
    private String cockpitURL;

    @Lazy
    @Autowired
    private InstallationRepository installationRepository;

    @Override
    public InstallationEntity get() {
        try {
            final Optional<Installation> optInstallation = this.installationRepository.find();
            if (optInstallation.isPresent()) {
                return convert(optInstallation.get());
            }
        } catch (final Exception ex) {
            throw new TechnicalManagementException("Error while getting installation", ex);
        }
        throw new InstallationNotFoundException("");
    }

    @Override
    public synchronized InstallationEntity getOrInitialize() {
        try {
            final Optional<Installation> optInstallation = this.installationRepository.find();
            if (optInstallation.isPresent()) {
                return convert(optInstallation.get());
            }
            return createInstallation();
        } catch (final Exception ex) {
            throw new TechnicalManagementException("Error while getting installation", ex);
        }
    }

    @Override
    public InstallationEntity setAdditionalInformation(Map<String, String> additionalInformation) {
        try {
            final Optional<Installation> optInstallation = this.installationRepository.find();
            if (optInstallation.isPresent()) {
                Installation installation = optInstallation.get();
                installation.setAdditionalInformation(additionalInformation);
                installation.setUpdatedAt(new Date());
                return convert(this.installationRepository.update(installation));
            }
        } catch (final Exception ex) {
            throw new TechnicalManagementException("Error while updating installation", ex);
        }
        throw new InstallationNotFoundException("");
    }

    @Override
    public InstallationStatus getInstallationStatus() {
        final String statusStr = getOrInitialize().getAdditionalInformation().get(COCKPIT_INSTALLATION_STATUS);

        if (statusStr != null) {
            return InstallationStatus.valueOf(statusStr);
        }

        return InstallationStatus.NOT_LINKED;
    }

    private InstallationEntity createInstallation() {
        final Date now = Date.from(Instant.now());
        final Installation installation = new Installation();
        installation.setId(UuidString.generateRandom());
        installation.setCreatedAt(now);
        installation.setUpdatedAt(now);
        installation.setAdditionalInformation(new HashMap<>());

        try {
            return convert(this.installationRepository.create(installation));
        } catch (final Exception ex) {
            throw new TechnicalManagementException("Error while creating installation", ex);
        }
    }

    InstallationEntity convert(Installation installation) {
        InstallationEntity result = new InstallationEntity();
        result.setId(installation.getId());
        result.setCockpitURL(this.cockpitURL);
        result.setCreatedAt(installation.getCreatedAt());
        result.setUpdatedAt(installation.getUpdatedAt());
        result.setAdditionalInformation(installation.getAdditionalInformation());

        return result;
    }
}
