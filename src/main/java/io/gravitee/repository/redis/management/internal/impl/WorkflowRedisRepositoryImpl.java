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

import io.gravitee.repository.redis.management.internal.WorkflowRedisRepository;
import io.gravitee.repository.redis.management.model.RedisWorkflow;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class WorkflowRedisRepositoryImpl extends AbstractRedisRepository implements WorkflowRedisRepository {

    private final static String REDIS_KEY = "workflow";

    @Override
    public Set<RedisWorkflow> findAll() {
        final Map<Object, Object> workflows = redisTemplate.opsForHash().entries(REDIS_KEY);

        return workflows.values()
                .stream()
                .map(object -> convert(object, RedisWorkflow.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisWorkflow findById(final String workflowId) {
        Object workflow = redisTemplate.opsForHash().get(REDIS_KEY, workflowId);
        if (workflow == null) {
            return null;
        }

        return convert(workflow, RedisWorkflow.class);
    }

    @Override
    public RedisWorkflow saveOrUpdate(final RedisWorkflow workflow) {
        redisTemplate.executePipelined((RedisConnection connection) ->  {
            final String refKey = getRefKey(workflow.getReferenceType(), workflow.getReferenceId(), workflow.getType());
            redisTemplate.opsForHash().put(REDIS_KEY, workflow.getId(), workflow);
            redisTemplate.opsForSet().add(refKey, workflow.getId());
            return null;
        });
        return workflow;
    }

    @Override
    public void delete(final String workflow) {
        redisTemplate.opsForHash().delete(REDIS_KEY, workflow);
    }

    @Override
    public List<RedisWorkflow> findByReferenceAndType(String referenceType, String referenceId, String type) {
        final Set<Object> keys = redisTemplate.opsForSet().members(getRefKey(referenceType, referenceId, type));
        final List<Object> values = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);
        return values.stream()
                .filter(Objects::nonNull)
                .map(workflow -> convert(workflow, RedisWorkflow.class))
                .sorted(comparing(RedisWorkflow::getCreatedAt).reversed())
                .collect(toList());
    }

    private String getRefKey(String referenceType, String referenceId, String type) {
        return referenceType + ':' + referenceId + ':' + type;
    }
}
