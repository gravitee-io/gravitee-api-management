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
package io.gravitee.repository.mongodb.management;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.ApiQualityRule;
import io.gravitee.repository.mongodb.management.internal.model.ApiQualityRuleMongo;
import io.gravitee.repository.mongodb.management.internal.model.ApiQualityRulePkMongo;
import io.gravitee.repository.mongodb.management.internal.quality.ApiQualityRuleMongoRepository;
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
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoApiQualityRuleRepository implements ApiQualityRuleRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoApiQualityRuleRepository.class);

    @Autowired
    private ApiQualityRuleMongoRepository internalApiQualityRuleRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<ApiQualityRule> findById(String api, String qualityRuleId) throws TechnicalException {
        LOGGER.debug("Find quality rule by ID [{}]", qualityRuleId);
        final ApiQualityRuleMongo apiQualityRule = internalApiQualityRuleRepo
            .findById(new ApiQualityRulePkMongo(api, qualityRuleId))
            .orElse(null);
        LOGGER.debug("Find quality rule by ID [{}] - Done", qualityRuleId);
        return Optional.ofNullable(mapper.map(apiQualityRule, ApiQualityRule.class));
    }

    @Override
    public ApiQualityRule create(ApiQualityRule apiQualityRule) throws TechnicalException {
        LOGGER.debug("Create quality rule for api [{}] and quality rule [{}]", apiQualityRule.getApi(), apiQualityRule.getQualityRule());
        ApiQualityRuleMongo apiQualityRuleMongo = mapper.map(apiQualityRule, ApiQualityRuleMongo.class);
        ApiQualityRuleMongo createdApiQualityRuleMongo = internalApiQualityRuleRepo.insert(apiQualityRuleMongo);
        ApiQualityRule res = mapper.map(createdApiQualityRuleMongo, ApiQualityRule.class);
        LOGGER.debug(
            "Create quality rule for api [{}] and quality rule [{}] - Done",
            apiQualityRule.getApi(),
            apiQualityRule.getQualityRule()
        );
        return res;
    }

    @Override
    public ApiQualityRule update(ApiQualityRule apiQualityRule) throws TechnicalException {
        if (apiQualityRule == null || apiQualityRule.getApi() == null || apiQualityRule.getQualityRule() == null) {
            throw new IllegalStateException("ApiQualityRule to update must have an api and a quality rule");
        }

        final ApiQualityRulePkMongo id = new ApiQualityRulePkMongo(apiQualityRule.getApi(), apiQualityRule.getQualityRule());
        final ApiQualityRuleMongo apiQualityRuleMongo = internalApiQualityRuleRepo.findById(id).orElse(null);

        if (apiQualityRuleMongo == null) {
            throw new IllegalStateException(
                format(
                    "No api quality rule found with api [%s] and quality rule [%s]",
                    apiQualityRule.getApi(),
                    apiQualityRule.getQualityRule()
                )
            );
        }

        try {
            apiQualityRuleMongo.setChecked(apiQualityRule.isChecked());
            apiQualityRuleMongo.setCreatedAt(apiQualityRule.getCreatedAt());
            apiQualityRuleMongo.setUpdatedAt(apiQualityRule.getUpdatedAt());

            ApiQualityRuleMongo apiQualityRuleMongoUpdated = internalApiQualityRuleRepo.save(apiQualityRuleMongo);
            return mapper.map(apiQualityRuleMongoUpdated, ApiQualityRule.class);
        } catch (Exception e) {
            final String error = "An error occurred when updating quality rule";
            LOGGER.error(error, e);
            throw new TechnicalException(error);
        }
    }

    @Override
    public void delete(String api, String qualityRule) throws TechnicalException {
        try {
            internalApiQualityRuleRepo.deleteById(new ApiQualityRulePkMongo(api, qualityRule));
        } catch (Exception e) {
            final String error = format(
                "An error occurred when deleting api quality rule with api [%s] and quality rule [%s]",
                api,
                qualityRule
            );
            LOGGER.error(error, e);
            throw new TechnicalException(error);
        }
    }

    @Override
    public List<ApiQualityRule> findByApi(String api) {
        final List<ApiQualityRuleMongo> apiQualityRules = internalApiQualityRuleRepo.findByIdApi(api);
        return apiQualityRules.stream().map(apiQualityRuleMongo -> mapper.map(apiQualityRuleMongo, ApiQualityRule.class)).collect(toList());
    }

    @Override
    public List<ApiQualityRule> findByQualityRule(String qualityRule) {
        final List<ApiQualityRuleMongo> apiQualityRules = internalApiQualityRuleRepo.findByIdQualityRule(qualityRule);
        return apiQualityRules.stream().map(apiQualityRuleMongo -> mapper.map(apiQualityRuleMongo, ApiQualityRule.class)).collect(toList());
    }

    @Override
    public void deleteByQualityRule(String qualityRule) throws TechnicalException {
        try {
            internalApiQualityRuleRepo.deleteByIdQualityRule(qualityRule);
        } catch (Exception e) {
            final String error = format("An error occurred when deleting api quality rule by quality rule [%s]", qualityRule);
            LOGGER.error(error, e);
            throw new TechnicalException(error);
        }
    }

    @Override
    public void deleteByApi(String api) throws TechnicalException {
        try {
            internalApiQualityRuleRepo.deleteByIdApi(api);
        } catch (Exception e) {
            final String error = format("An error occurred when deleting api quality rule by api [%s]", api);
            LOGGER.error(error, e);
            throw new TechnicalException(error);
        }
    }

    @Override
    public Set<ApiQualityRule> findAll() throws TechnicalException {
        return internalApiQualityRuleRepo
            .findAll()
            .stream()
            .map(apiQualityRuleMongo -> mapper.map(apiQualityRuleMongo, ApiQualityRule.class))
            .collect(Collectors.toSet());
    }
}
