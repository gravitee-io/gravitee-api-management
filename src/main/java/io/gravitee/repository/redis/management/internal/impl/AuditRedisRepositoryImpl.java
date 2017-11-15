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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.redis.management.internal.AuditRedisRepository;
import io.gravitee.repository.redis.management.model.RedisAudit;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AuditRedisRepositoryImpl extends AbstractRedisRepository implements AuditRedisRepository {

    private final static String REDIS_KEY = "audit";

    @Override
    public RedisAudit find(String id) {
        Object audit = redisTemplate.opsForHash().get(REDIS_KEY, id);
        if (audit == null) {
            return null;
        }

        return convert(audit, RedisAudit.class);
    }

    @Override
    public RedisAudit saveOrUpdate(RedisAudit audit) {
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                redisTemplate.opsForHash().put(REDIS_KEY, audit.getId(), audit);
                redisTemplate.opsForSet().add(REDIS_KEY + ":event:" + audit.getEvent(), audit.getId());
                redisTemplate.opsForSet().add(REDIS_KEY + ":reference_type:" + audit.getReferenceType(), audit.getId());
                redisTemplate.opsForSet().add(REDIS_KEY + ":reference:" + audit.getReferenceId(), audit.getId());
                redisTemplate.opsForZSet().add(REDIS_KEY + ":created_at", audit.getId(), audit.getCreatedAt());
                return null;
            }
        });

        return audit;
    }

    @Override
    public Page<RedisAudit> search(AuditCriteria filter, Pageable pageable) {
        Set<String> filterKeys = new HashSet<>();
        String tempDestination = "tmp-" + Math.abs(filter.hashCode());

        // Implement OR clause for event type
        if (filter.getEvents() != null && ! filter.getEvents().isEmpty()) {
            filter.getEvents().forEach(event -> filterKeys.add(REDIS_KEY + ":event:" + event));
            redisTemplate.opsForZSet().unionAndStore(null, filterKeys, tempDestination);
            filterKeys.clear();
            filterKeys.add(tempDestination);
        }

        if (filter.getReferences() != null && ! filter.getReferences().isEmpty()) {
            Map.Entry<Audit.AuditReferenceType, List<String>> reference = filter.getReferences().entrySet().iterator().next();
            filterKeys.add(REDIS_KEY + ":reference_type:" + reference.getKey().name());
            reference.getValue().forEach(value -> filterKeys.add(REDIS_KEY + ":reference:" + value));
            redisTemplate.opsForZSet().intersectAndStore(null, filterKeys, tempDestination);
            filterKeys.clear();
            filterKeys.add(tempDestination);
        }

        // And finally add clause based on audit creation date
        filterKeys.add(REDIS_KEY + ":created_at");

        redisTemplate.opsForZSet().intersectAndStore(null, filterKeys, tempDestination);

        Set<Object> keys = redisTemplate.opsForZSet().reverseRangeByScore(
                tempDestination,
                filter.getFrom(),
                filter.getTo() == 0 ? Long.MAX_VALUE : filter.getTo());

        long count = keys.size();

        if (pageable != null) {
            keys = redisTemplate.opsForZSet().reverseRangeByScore(
                    tempDestination,
                    filter.getFrom(),
                    filter.getTo() == 0 ? Long.MAX_VALUE : filter.getTo(),
                    pageable.pageNumber() - 1, pageable.pageSize());
        } else {
            keys = redisTemplate.opsForZSet().reverseRangeByScore(
                    tempDestination,
                    filter.getFrom(),
                    filter.getTo() == 0 ? Long.MAX_VALUE : filter.getTo());
        }

        redisTemplate.opsForZSet().removeRange(tempDestination, 0, -1);
        List<Object> apiKeysObject = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return new Page<>(
                apiKeysObject.stream()
                        .map(apiKey -> convert(apiKey, RedisAudit.class))
                        .collect(Collectors.toList()),
                            (pageable != null) ? pageable.pageNumber() : 0,
                            (pageable != null) ? pageable.pageSize() : 0,
                count);
    }

}
