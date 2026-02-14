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

import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.model.Command;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandQuery;
import io.gravitee.rest.api.model.command.NewCommandEntity;
import io.gravitee.rest.api.service.CommandService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.CommandConverter;
import io.gravitee.rest.api.service.exceptions.Message2RecipientNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class CommandServiceImpl extends AbstractService implements CommandService {

    @Lazy
    @Autowired
    CommandRepository commandRepository;

    @Autowired
    CommandConverter commandConverter;

    @Autowired
    Node node;

    @Override
    public void send(ExecutionContext executionContext, NewCommandEntity commandEntity) {
        if (commandEntity.getTo() == null || commandEntity.getTo().isEmpty()) {
            throw new Message2RecipientNotFoundException();
        }

        Command command = commandConverter.toCommand(executionContext, commandEntity);

        try {
            commandRepository.create(command);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to create {}", command, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + command, ex);
        }
    }

    @Override
    public List<CommandEntity> search(CommandQuery query) {
        return search(null, query);
    }

    @Override
    public List<CommandEntity> search(ExecutionContext executionContext, CommandQuery query) {
        //convert tags
        String[] tags = null;
        if (query.getTags() != null) {
            tags = query.getTags().stream().map(Enum::name).toArray(String[]::new);
        }

        final CommandCriteria.Builder criteriaBuilder = new CommandCriteria.Builder().to(query.getTo()).tags(tags);
        if (executionContext != null) {
            criteriaBuilder
                .organizationId(executionContext.getOrganizationId())
                .environmentId(executionContext.hasEnvironmentId() ? executionContext.getEnvironmentId() : null);
        }
        CommandCriteria criteria = criteriaBuilder.notAckBy(query.getNotAckBy()).build();

        return commandRepository.search(criteria).stream().map(commandConverter::toCommandEntity).collect(Collectors.toList());
    }

    @Override
    public void ack(String messageId) {
        try {
            Optional<Command> optMsg = commandRepository.findById(messageId);
            //if not found, this is probably because it has been deleted
            if (optMsg.isPresent()) {
                Command msg = optMsg.get();
                if (msg.getAcknowledgments() == null) {
                    msg.setAcknowledgments(Collections.singletonList(node.id()));
                } else if (!msg.getAcknowledgments().contains(node.id())) {
                    msg.getAcknowledgments().add(node.id());
                }
                commandRepository.update(msg);
            }
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to acknowledge a message", ex);
        }
    }

    @Override
    public void delete(String commandId) {
        try {
            commandRepository.delete(commandId);
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to delete command " + commandId;
            log.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public int deleteByExpiredAtBefore(Instant before) {
        try {
            int deletedCount = commandRepository.deleteByExpiredAtBefore(before);
            log.debug("Deleted {} expired commands before {}", deletedCount, before);
            return deletedCount;
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to delete expired commands", ex);
        }
    }
}
