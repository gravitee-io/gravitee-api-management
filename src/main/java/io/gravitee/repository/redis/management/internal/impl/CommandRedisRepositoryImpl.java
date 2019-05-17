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
package io.gravitee.repository.redis.management.internal.impl;

import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.redis.management.internal.CommandRedisRepository;
import io.gravitee.repository.redis.management.model.RedisCommand;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class CommandRedisRepositoryImpl extends AbstractRedisRepository implements CommandRedisRepository {

    private static final String REDIS_KEY = "command";

    @Override
    public RedisCommand findById(final String commandId) {
        Object command = redisTemplate.opsForHash().get(REDIS_KEY, commandId);
        if (command == null) {
            return null;
        }
        return convert(command, RedisCommand.class);
    }

    @Override
    public RedisCommand saveOrUpdate(final RedisCommand command) {
        redisTemplate.opsForHash().put(REDIS_KEY, command.getId(), command);
        return command;
    }

    @Override
    public void delete(final String commandId) {
        redisTemplate.opsForHash().delete(REDIS_KEY, commandId);
    }

    @Override
    public List<RedisCommand> search(CommandCriteria criteria) {
        Map<Object, Object> commands = redisTemplate.opsForHash().entries(REDIS_KEY);

        return commands.values()
                .stream()
                .map(object -> convert(object, RedisCommand.class))
                .filter( command -> {
                    boolean filtered = true;
                    if (criteria.getTags() != null && criteria.getTags().length > 0) {
                        filtered = command.getTags() != null && command.getTags().containsAll(Arrays.asList(criteria.getTags()));
                    }
                    if (filtered && criteria.getNotAckBy() != null) {
                        filtered = command.getAcknowledgments() == null || !command.getAcknowledgments().contains(criteria.getNotAckBy());
                    }
                    if (filtered && criteria.getNotFrom() != null) {
                        filtered = command.getFrom() == null || !command.getFrom().equals(criteria.getNotFrom());
                    }
                    if (filtered && criteria.getTo() != null) {
                        filtered = command.getTo() != null && command.getTo().equals(criteria.getTo());
                    }
                    if (filtered && criteria.isNotExpired()) {
                        filtered = command.getExpiredAt() == null || command.getExpiredAt().after(new Date());
                    }
                    return filtered;
                })
                .collect(Collectors.toList());


    }
}
