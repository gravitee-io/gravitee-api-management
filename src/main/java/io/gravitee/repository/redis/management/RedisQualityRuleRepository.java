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
import io.gravitee.repository.management.api.QualityRuleRepository;
import io.gravitee.repository.management.model.QualityRule;
import io.gravitee.repository.redis.management.internal.QualityRuleRedisRepository;
import io.gravitee.repository.redis.management.model.RedisQualityRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisQualityRuleRepository implements QualityRuleRepository {

    @Autowired
    private QualityRuleRedisRepository qualityRuleRedisRepository;

    @Override
    public Optional<QualityRule> findById(final String qualityRuleId) throws TechnicalException {
        final RedisQualityRule redisQualityRule = qualityRuleRedisRepository.findById(qualityRuleId);
        return Optional.ofNullable(convert(redisQualityRule));
    }

    @Override
    public QualityRule create(final QualityRule qualityRule) throws TechnicalException {
        final RedisQualityRule redisQualityRule = qualityRuleRedisRepository.saveOrUpdate(convert(qualityRule));
        return convert(redisQualityRule);
    }

    @Override
    public QualityRule update(final QualityRule qualityRule) throws TechnicalException {
        if (qualityRule == null || qualityRule.getName() == null) {
            throw new IllegalStateException("QualityRule to update must have a name");
        }

        final RedisQualityRule redisQualityRule = qualityRuleRedisRepository.findById(qualityRule.getId());

        if (redisQualityRule == null) {
            throw new IllegalStateException(String.format("No qualityRule found with name [%s]", qualityRule.getId()));
        }

        final RedisQualityRule redisQualityRuleUpdated = qualityRuleRedisRepository.saveOrUpdate(convert(qualityRule));
        return convert(redisQualityRuleUpdated);
    }

    @Override
    public Set<QualityRule> findAll() throws TechnicalException {
        final List<RedisQualityRule> qualityRules = qualityRuleRedisRepository.findAll();

        return qualityRules.stream()
                .map(this::convert)
                .collect(toSet());
    }

    @Override
    public void delete(final String qualityRuleId) throws TechnicalException {
        qualityRuleRedisRepository.delete(qualityRuleId);
    }

    private QualityRule convert(final RedisQualityRule redisQualityRule) {
        if (redisQualityRule == null) {
            return null;
        }
        final QualityRule qualityRule = new QualityRule();
        qualityRule.setId(redisQualityRule.getId());
        qualityRule.setName(redisQualityRule.getName());
        qualityRule.setDescription(redisQualityRule.getDescription());
        qualityRule.setWeight(redisQualityRule.getWeight());
        qualityRule.setCreatedAt(redisQualityRule.getCreatedAt());
        qualityRule.setUpdatedAt(redisQualityRule.getUpdatedAt());
        return qualityRule;
    }

    private RedisQualityRule convert(final QualityRule qualityRule) {
        if (qualityRule == null) {
            return null;
        }
        final RedisQualityRule redisQualityRule = new RedisQualityRule();
        redisQualityRule.setId(qualityRule.getId());
        redisQualityRule.setName(qualityRule.getName());
        redisQualityRule.setDescription(qualityRule.getDescription());
        redisQualityRule.setWeight(qualityRule.getWeight());
        redisQualityRule.setCreatedAt(qualityRule.getCreatedAt());
        redisQualityRule.setUpdatedAt(qualityRule.getUpdatedAt());
        return redisQualityRule;
    }
}
