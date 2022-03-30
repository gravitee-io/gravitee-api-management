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

import static io.gravitee.repository.management.model.Audit.AuditProperties.API_QUALITY_RULE;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.model.ApiQualityRule;
import io.gravitee.rest.api.model.quality.*;
import io.gravitee.rest.api.service.ApiQualityRuleService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.*;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiQualityRuleServiceImpl extends AbstractService implements ApiQualityRuleService {

    private final Logger LOGGER = LoggerFactory.getLogger(ApiQualityRuleServiceImpl.class);

    @Autowired
    private ApiQualityRuleRepository apiQualityRuleRepository;

    @Autowired
    private AuditService auditService;

    @Override
    public List<ApiQualityRuleEntity> findByApi(final String api) {
        try {
            LOGGER.debug("Find quality rules by API");
            return apiQualityRuleRepository.findByApi(api).stream().map(this::convert).collect(toList());
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find quality rules by API";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public ApiQualityRuleEntity create(ExecutionContext executionContext, NewApiQualityRuleEntity newEntity) {
        try {
            final Optional<ApiQualityRule> optionalApiQualityRule = apiQualityRuleRepository.findById(
                newEntity.getApi(),
                newEntity.getQualityRule()
            );
            if (optionalApiQualityRule.isPresent()) {
                throw new ApiQualityRuleAlreadyExistsException(newEntity.getApi(), newEntity.getQualityRule());
            }
            final ApiQualityRule apiQualityRule = convert(newEntity);
            auditService.createEnvironmentAuditLog(
                executionContext,
                executionContext.getEnvironmentId(),
                Collections.singletonMap(API_QUALITY_RULE, apiQualityRule.getApi()),
                ApiQualityRule.AuditEvent.API_QUALITY_RULE_CREATED,
                apiQualityRule.getCreatedAt(),
                null,
                apiQualityRule
            );
            return convert(apiQualityRuleRepository.create(apiQualityRule));
        } catch (TechnicalException e) {
            final String error = "An error occurs while trying to create an API quality rule " + newEntity;
            LOGGER.error(error, e);
            throw new TechnicalManagementException(error, e);
        }
    }

    @Override
    public ApiQualityRuleEntity update(ExecutionContext executionContext, UpdateApiQualityRuleEntity updateEntity) {
        try {
            final Optional<ApiQualityRule> optionalApiQualityRule = apiQualityRuleRepository.findById(
                updateEntity.getApi(),
                updateEntity.getQualityRule()
            );
            if (!optionalApiQualityRule.isPresent()) {
                throw new ApiQualityRuleNotFoundException(updateEntity.getApi(), updateEntity.getQualityRule());
            }
            final ApiQualityRule apiQualityRule = apiQualityRuleRepository.update(convert(updateEntity));
            auditService.createEnvironmentAuditLog(
                executionContext,
                executionContext.getEnvironmentId(),
                singletonMap(API_QUALITY_RULE, apiQualityRule.getApi()),
                ApiQualityRule.AuditEvent.API_QUALITY_RULE_UPDATED,
                apiQualityRule.getUpdatedAt(),
                optionalApiQualityRule.get(),
                apiQualityRule
            );
            return convert(apiQualityRule);
        } catch (TechnicalException e) {
            final String error = "An error occurs while trying to update API quality rule " + updateEntity;
            LOGGER.error(error, e);
            throw new TechnicalManagementException(error, e);
        }
    }

    private ApiQualityRuleEntity convert(ApiQualityRule apiQualityRule) {
        ApiQualityRuleEntity entity = new ApiQualityRuleEntity();
        entity.setApi(apiQualityRule.getApi());
        entity.setQualityRule(apiQualityRule.getQualityRule());
        entity.setChecked(apiQualityRule.isChecked());
        entity.setCreatedAt(apiQualityRule.getCreatedAt());
        entity.setUpdatedAt(apiQualityRule.getUpdatedAt());
        return entity;
    }

    private ApiQualityRule convert(final NewApiQualityRuleEntity apiQualityRuleEntity) {
        final ApiQualityRule apiQualityRule = new ApiQualityRule();
        apiQualityRule.setApi(apiQualityRuleEntity.getApi());
        apiQualityRule.setQualityRule(apiQualityRuleEntity.getQualityRule());
        apiQualityRule.setChecked(apiQualityRuleEntity.isChecked());
        final Date now = new Date();
        apiQualityRule.setCreatedAt(now);
        apiQualityRule.setUpdatedAt(now);
        return apiQualityRule;
    }

    private ApiQualityRule convert(final UpdateApiQualityRuleEntity apiQualityRuleEntity) {
        final ApiQualityRule apiQualityRule = new ApiQualityRule();
        apiQualityRule.setApi(apiQualityRuleEntity.getApi());
        apiQualityRule.setQualityRule(apiQualityRuleEntity.getQualityRule());
        apiQualityRule.setChecked(apiQualityRuleEntity.isChecked());
        final Date now = new Date();
        apiQualityRule.setCreatedAt(now);
        apiQualityRule.setUpdatedAt(now);
        return apiQualityRule;
    }
}
