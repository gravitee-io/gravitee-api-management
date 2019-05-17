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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.redis.management.internal.CommandRedisRepository;
import io.gravitee.repository.redis.management.model.RedisCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class RedisCommandRepository implements CommandRepository {

    @Autowired
    private CommandRedisRepository commandRedisRepository;

    @Override
    public Optional<Command> findById(final String commandId) throws TechnicalException {
        final RedisCommand redisCommand = commandRedisRepository.findById(commandId);
        return Optional.ofNullable(convert(redisCommand));
    }

    @Override
    public Command create(final Command command) throws TechnicalException {
        final RedisCommand redisCommand = commandRedisRepository.saveOrUpdate(convert(command));
        return convert(redisCommand);
    }

    @Override
    public Command update(final Command command) throws TechnicalException {
        if (command == null || command.getId() == null) {
            throw new IllegalStateException("Command to update must have an id");
        }

        final RedisCommand redisCommand = commandRedisRepository.findById(command.getId());

        if (redisCommand == null) {
            throw new IllegalStateException(String.format("No found found with id [%s]", command.getId()));
        }

        final RedisCommand redisCommandUpdated = commandRedisRepository.saveOrUpdate(convert(command));
        return convert(redisCommandUpdated);
    }

    @Override
    public void delete(final String commandId) throws TechnicalException {
        commandRedisRepository.delete(commandId);
    }

    @Override
    public List<Command> search(CommandCriteria criteria) {
        return commandRedisRepository.search(criteria).stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    private Command convert(final RedisCommand redisCommand) {
        if (redisCommand == null) {
            return null;
        }
        final Command command = new Command();
        command.setId(redisCommand.getId());
        command.setContent(redisCommand.getContent());
        command.setCreatedAt(redisCommand.getCreatedAt());
        command.setUpdatedAt(redisCommand.getUpdatedAt());
        command.setExpiredAt(redisCommand.getExpiredAt());
        command.setFrom(redisCommand.getFrom());
        command.setTo(redisCommand.getTo());
        command.setTags(redisCommand.getTags());
        command.setAcknowledgments(redisCommand.getAcknowledgments());
        return command;
    }

    private RedisCommand convert(final Command command) {
        if (command == null) {
            return null;
        }
        final RedisCommand redisCommand = new RedisCommand();
        redisCommand.setId(command.getId());
        redisCommand.setContent(command.getContent());
        redisCommand.setCreatedAt(command.getCreatedAt());
        redisCommand.setUpdatedAt(command.getUpdatedAt());
        redisCommand.setExpiredAt(command.getExpiredAt());
        redisCommand.setFrom(command.getFrom());
        redisCommand.setTo(command.getTo());
        redisCommand.setTags(command.getTags());
        redisCommand.setAcknowledgments(command.getAcknowledgments());
        return redisCommand;
    }
}
