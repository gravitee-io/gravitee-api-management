/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.service.InstallationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class CommandOrganizationUpgrader extends OneShotUpgrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultApiHeaderUpgrader.class);

    private final EnvironmentRepository environmentRepository;
    private final CommandRepository commandRepository;

    @Autowired
    public CommandOrganizationUpgrader(@Lazy EnvironmentRepository environmentRepository, @Lazy CommandRepository commandRepository) {
        super(InstallationService.COMMAND_ORGANIZATION_UPGRADER);
        this.environmentRepository = environmentRepository;
        this.commandRepository = commandRepository;
    }

    @Override
    public int getOrder() {
        return 500;
    }

    @Override
    protected void processOneShotUpgrade() throws Exception {
        environmentRepository.findAll().forEach(this::updateCommands);
    }

    private void updateCommands(Environment environment) {
        CommandCriteria criteria = new CommandCriteria.Builder().environmentId(environment.getId()).build();
        commandRepository.search(criteria).forEach(command -> updateOrganizationId(command, environment.getOrganizationId()));
    }

    private void updateOrganizationId(Command command, String organizationId) {
        try {
            command.setOrganizationId(organizationId);
            commandRepository.update(command);
        } catch (TechnicalException e) {
            LOGGER.error("An error as occurred while trying to update command organization", e);
        }
    }
}
