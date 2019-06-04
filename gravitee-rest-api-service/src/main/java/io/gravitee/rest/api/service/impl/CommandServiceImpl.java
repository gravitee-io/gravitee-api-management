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
package io.gravitee.rest.api.service.impl;

import io.gravitee.common.utils.UUID;
import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.model.Command;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandQuery;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.model.command.NewCommandEntity;
import io.gravitee.rest.api.service.CommandService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.Message2RecipientNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class CommandServiceImpl extends AbstractService implements CommandService {

    private final Logger logger = LoggerFactory.getLogger(CommandServiceImpl.class);

    @Autowired
    CommandRepository commandRepository;

    @Autowired
    Node node;

    @Override
    public void send(NewCommandEntity messageEntity) {
        if (messageEntity.getTo() == null || messageEntity.getTo().isEmpty()) {
            throw new Message2RecipientNotFoundException();
        }

        Command command = new Command();
        command.setId(UUID.toString(java.util.UUID.randomUUID()));
        command.setEnvironment(GraviteeContext.getCurrentEnvironment());
        command.setFrom(node.id());
        command.setTo(messageEntity.getTo());
        command.setTags(convert(messageEntity.getTags()));
        long now = System.currentTimeMillis();
        command.setCreatedAt(new Date(now));
        command.setUpdatedAt(command.getCreatedAt());
        command.setExpiredAt(new Date(now + (messageEntity.getTtlInSeconds() * 1000)));
        if (messageEntity.getContent() != null) {
            command.setContent(messageEntity.getContent());
        }

        try {
            commandRepository.create(command);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to create {}", command, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + command, ex);
        }
    }

    @Override
    public List<CommandEntity> search(CommandQuery query) {
        //convert tags
        String[] tags = null;
        if (query.getTags() != null) {
            tags = query.getTags()
                    .stream()
                    .map(Enum::name)
                    .toArray(String[]::new);
        }
        CommandCriteria criteria = new CommandCriteria.Builder()
                .to(query.getTo())
                .tags(tags)
                .notAckBy(node.id())
                .notDeleted()
                .environment(GraviteeContext.getCurrentEnvironment())
                .build();
        return commandRepository.search(criteria)
                .stream()
                .map(this::map)
                .collect(Collectors.toList());
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
                } else {
                    msg.getAcknowledgments().add(node.id());
                }
                commandRepository.update(msg);
            }
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to acknowledge a message", ex);
        }
    }

    private List<String> convert(List<CommandTags> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }

        return tags
                .stream()
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    private CommandEntity map(Command command) {
        if (command == null) {
            return null;
        }

        CommandEntity commandEntity = new CommandEntity();

        commandEntity.setId(command.getId());
        commandEntity.setTo(command.getTo());
        commandEntity.setContent(command.getContent());
        if (command.getTags() != null && !command.getTags().isEmpty()) {
            commandEntity.setTags(
                    command.getTags()
                            .stream()
                            .map(CommandTags::valueOf)
                            .collect(Collectors.toList()));
        }

        return commandEntity;
    }
}
