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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.management.model.Environment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class CommandOrganizationUpgrader implements Upgrader {

    private final EnvironmentRepository environmentRepository;
    private final CommandRepository commandRepository;

    @Autowired
    public CommandOrganizationUpgrader(@Lazy EnvironmentRepository environmentRepository, @Lazy CommandRepository commandRepository) {
        this.environmentRepository = environmentRepository;
        this.commandRepository = commandRepository;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.COMMAND_ORGANIZATION_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(() -> {
                for (Environment environment : environmentRepository.findAll()) {
                    updateCommands(environment);
                }
                return true;
            });
    }

    private void updateCommands(Environment environment) throws TechnicalException {
        CommandCriteria criteria = new CommandCriteria.Builder().environmentId(environment.getId()).build();
        for (Command command : commandRepository.search(criteria)) {
            updateOrganizationId(command, environment.getOrganizationId());
        }
    }

    private void updateOrganizationId(Command command, String organizationId) throws TechnicalException {
        command.setOrganizationId(organizationId);
        commandRepository.update(command);
    }
}
