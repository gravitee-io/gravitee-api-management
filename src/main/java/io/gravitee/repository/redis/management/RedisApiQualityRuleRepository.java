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
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.model.ApiQualityRule;
import io.gravitee.repository.redis.management.internal.ApiQualityRuleRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApiQualityRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisApiQualityRuleRepository implements ApiQualityRuleRepository {

    @Autowired
    private ApiQualityRuleRedisRepository apiQualityRuleRedisRepository;

    @Override
    public Optional<ApiQualityRule> findById(String api, String qualityRule) throws TechnicalException {
        final RedisApiQualityRule redisApiQualityRule = apiQualityRuleRedisRepository.findById(api, qualityRule);
        return Optional.ofNullable(convert(redisApiQualityRule));
    }

    @Override
    public ApiQualityRule create(final ApiQualityRule apiQualityRule) throws TechnicalException {
        final RedisApiQualityRule redisApiQualityRule = apiQualityRuleRedisRepository.saveOrUpdate(convert(apiQualityRule));
        return convert(redisApiQualityRule);
    }

    @Override
    public ApiQualityRule update(final ApiQualityRule apiQualityRule) throws TechnicalException {
        if (apiQualityRule == null || apiQualityRule.getApi() == null || apiQualityRule.getQualityRule() == null) {
            throw new IllegalStateException("ApiQualityRule to update must have an api and a quality rule");
        }

        final RedisApiQualityRule redisApiQualityRule = apiQualityRuleRedisRepository.findById(apiQualityRule.getApi(), apiQualityRule.getQualityRule());

        if (redisApiQualityRule == null) {
            throw new IllegalStateException(String.format("No apiQualityRule found with api [%s] and quality rule [%s]", apiQualityRule.getApi(), apiQualityRule.getQualityRule()));
        }

        final RedisApiQualityRule redisApiQualityRuleUpdated = apiQualityRuleRedisRepository.saveOrUpdate(convert(apiQualityRule));
        return convert(redisApiQualityRuleUpdated);
    }

    @Override
    public List<ApiQualityRule> findByApi(String api) throws TechnicalException {
        final List<RedisApiQualityRule> apiQualityRules = apiQualityRuleRedisRepository.findByApi(api);
        return apiQualityRules.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiQualityRule> findByQualityRule(String qualityRule) throws TechnicalException {
        final List<RedisApiQualityRule> apiQualityRules = apiQualityRuleRedisRepository.findByQualityRule(qualityRule);
        return apiQualityRules.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(final String api, final String qualityRule) throws TechnicalException {
        apiQualityRuleRedisRepository.delete(api, qualityRule);
    }

    @Override
    public void deleteByQualityRule(String qualityRule) throws TechnicalException {
        apiQualityRuleRedisRepository.deleteByQualityRule(qualityRule);
    }

    @Override
    public void deleteByApi(String api) throws TechnicalException {
        apiQualityRuleRedisRepository.deleteByApi(api);
    }

    private ApiQualityRule convert(final RedisApiQualityRule redisApiQualityRule) {
        if (redisApiQualityRule == null) {
            return null;
        }
        final ApiQualityRule apiQualityRule = new ApiQualityRule();
        apiQualityRule.setApi(redisApiQualityRule.getApi());
        apiQualityRule.setQualityRule(redisApiQualityRule.getQualityRule());
        apiQualityRule.setChecked(redisApiQualityRule.isChecked());
        apiQualityRule.setCreatedAt(redisApiQualityRule.getCreatedAt());
        apiQualityRule.setUpdatedAt(redisApiQualityRule.getUpdatedAt());
        return apiQualityRule;
    }

    private RedisApiQualityRule convert(final ApiQualityRule apiQualityRule) {
        if (apiQualityRule == null) {
            return null;
        }
        final RedisApiQualityRule redisApiQualityRule = new RedisApiQualityRule();
        redisApiQualityRule.setApi(apiQualityRule.getApi());
        redisApiQualityRule.setQualityRule(apiQualityRule.getQualityRule());
        redisApiQualityRule.setChecked(apiQualityRule.isChecked());
        redisApiQualityRule.setCreatedAt(apiQualityRule.getCreatedAt());
        redisApiQualityRule.setUpdatedAt(apiQualityRule.getUpdatedAt());
        return redisApiQualityRule;
    }
}
