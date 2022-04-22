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
package io.gravitee.rest.api.service.converter;

import io.gravitee.node.api.Node;
import io.gravitee.repository.management.model.Command;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.model.command.NewCommandEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @author GraviteeSource Team
 */
@Component
public class CommandConverter {

    private final Node node;

    @Autowired
    public CommandConverter(Node node) {
        this.node = node;
    }

    public CommandEntity toCommandEntity(Command command) {
        if (command == null) {
            return null;
        }
        CommandEntity commandEntity = new CommandEntity();
        commandEntity.setId(command.getId());
        commandEntity.setOrganizationId(command.getOrganizationId());
        commandEntity.setEnvironmentId(command.getEnvironmentId());
        commandEntity.setTo(command.getTo());
        commandEntity.setContent(command.getContent());
        commandEntity.setTags(toCommandTags(command.getTags()));
        commandEntity.setExpired(command.getExpiredAt().before(new Date()));
        commandEntity.setProcessedInCurrentNode(isProcessedInCurrentNode(command));
        return commandEntity;
    }

    public Command toCommand(ExecutionContext executionContext, NewCommandEntity commandEntity) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(Duration.ofSeconds(commandEntity.getTtlInSeconds()));
        Command command = new Command();
        command.setId(UuidString.generateRandom());
        command.setOrganizationId(executionContext.getOrganizationId());
        command.setEnvironmentId(executionContext.hasEnvironmentId() ? executionContext.getEnvironmentId() : null);
        command.setFrom(node.id());
        command.setTo(commandEntity.getTo());
        command.setTags(toStrings(commandEntity.getTags()));
        command.setCreatedAt(Date.from(now));
        command.setUpdatedAt(Date.from(now));
        command.setExpiredAt(Date.from(expireAt));
        if (commandEntity.getContent() != null) {
            command.setContent(commandEntity.getContent());
        }
        return command;
    }

    private boolean isProcessedInCurrentNode(Command command) {
        final List<String> acknowledgments = command.getAcknowledgments();
        return acknowledgments != null && acknowledgments.contains(node.id());
    }

    private List<CommandTags> toCommandTags(List<String> commandTags) {
        if (CollectionUtils.isEmpty(commandTags)) {
            return null;
        }
        return commandTags.stream().map(CommandTags::valueOf).collect(Collectors.toList());
    }

    private List<String> toStrings(List<CommandTags> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }

        return tags.stream().map(Enum::name).collect(Collectors.toList());
    }
}
