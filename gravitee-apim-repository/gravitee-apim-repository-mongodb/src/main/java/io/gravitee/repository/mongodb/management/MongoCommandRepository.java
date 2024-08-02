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
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.mongodb.management.internal.message.CommandMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.CommandMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoCommandRepository implements CommandRepository {

    private final Logger logger = LoggerFactory.getLogger(MongoCommandRepository.class);

    @Autowired
    private CommandMongoRepository internalMessageRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Command> findById(String commandId) throws TechnicalException {
        logger.debug("Find Command by ID [{}]", commandId);

        final CommandMongo msg = internalMessageRepo.findById(commandId).orElse(null);

        logger.debug("Find Command by ID [{}] - Done", commandId);
        return Optional.ofNullable(mapper.map(msg));
    }

    @Override
    public Command create(Command command) throws TechnicalException {
        logger.debug("Create Command [{}]", command.getId());

        CommandMongo createdMsgMongo = internalMessageRepo.insert(mapper.map(command));

        Command res = mapper.map(createdMsgMongo);

        logger.debug("Create Command [{}] - Done", res.getId());

        return res;
    }

    @Override
    public Command update(Command command) throws TechnicalException {
        if (command == null || command.getId() == null) {
            throw new IllegalStateException("Tag to update must have an id");
        }

        final CommandMongo commandMongo = internalMessageRepo.findById(command.getId()).orElse(null);

        if (commandMongo == null) {
            throw new IllegalStateException(String.format("No command found with id [%s]", command.getId()));
        }

        try {
            CommandMongo commandMongoUpdated = internalMessageRepo.save(mapper.map(command));
            return mapper.map(commandMongoUpdated);
        } catch (Exception e) {
            logger.error("An error occurred when updating command", e);
            throw new TechnicalException("An error occurred when updating command");
        }
    }

    @Override
    public void delete(String commandId) throws TechnicalException {
        try {
            internalMessageRepo.deleteById(commandId);
        } catch (Exception e) {
            logger.error("An error occurred when deleting command [{}]", commandId, e);
            throw new TechnicalException("An error occurred when deleting command");
        }
    }

    @Override
    public List<Command> search(CommandCriteria criteria) {
        logger.debug("Search Command [{}]", criteria);
        List<CommandMongo> result = internalMessageRepo.search(criteria);
        return mapper.mapCommands(result);
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        logger.debug("Delete by environmentId [{}]", environmentId);
        try {
            final var rows = internalMessageRepo.deleteByEnvironmentId(environmentId).stream().map(CommandMongo::getId).toList();
            logger.debug("Delete by environmentId [{}] - Done", environmentId);
            return rows;
        } catch (Exception ex) {
            logger.error("Failed to delete commands by envId: {}", environmentId, ex);
            throw new TechnicalException("Failed to delete commands by envId");
        }
    }

    @Override
    public List<String> deleteByOrganizationId(String organizationId) throws TechnicalException {
        logger.debug("Delete by organizationId [{}]", organizationId);
        try {
            final var rows = internalMessageRepo.deleteByOrganizationId(organizationId).stream().map(CommandMongo::getId).toList();
            logger.debug("Delete by organizationId [{}] - Done", organizationId);
            return rows;
        } catch (Exception ex) {
            logger.error("Failed to delete commands by organizationId: {}", organizationId, ex);
            throw new TechnicalException("Failed to delete commands by organizationId");
        }
    }

    @Override
    public Set<Command> findAll() throws TechnicalException {
        return internalMessageRepo.findAll().stream().map(commandMongo -> mapper.map(commandMongo)).collect(Collectors.toSet());
    }
}
