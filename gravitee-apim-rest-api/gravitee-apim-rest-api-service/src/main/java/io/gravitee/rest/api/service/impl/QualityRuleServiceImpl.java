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

import static io.gravitee.repository.management.model.Audit.AuditProperties.QUALITY_RULE;
import static io.gravitee.repository.management.model.QualityRule.AuditEvent.QUALITY_RULE_UPDATED;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.api.QualityRuleRepository;
import io.gravitee.repository.management.model.QualityRule;
import io.gravitee.rest.api.model.quality.NewQualityRuleEntity;
import io.gravitee.rest.api.model.quality.QualityRuleEntity;
import io.gravitee.rest.api.model.quality.UpdateQualityRuleEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.QualityRuleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.QualityRuleNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
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
public class QualityRuleServiceImpl extends AbstractService implements QualityRuleService {

    private final Logger LOGGER = LoggerFactory.getLogger(QualityRuleServiceImpl.class);

    @Autowired
    private QualityRuleRepository qualityRuleRepository;

    @Autowired
    private ApiQualityRuleRepository apiQualityRuleRepository;

    @Autowired
    private AuditService auditService;

    @Override
    public QualityRuleEntity findById(String id) {
        try {
            LOGGER.debug("Find quality rule by id : {}", id);
            Optional<QualityRule> qualityRule = qualityRuleRepository.findById(id);
            if (qualityRule.isPresent()) {
                return convert(qualityRule.get());
            }
            throw new QualityRuleNotFoundException(id);
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find a quality rule using its ID: " + id;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public List<QualityRuleEntity> findAll() {
        try {
            LOGGER.debug("Find all quality rules");
            return qualityRuleRepository.findAll().stream().map(this::convert).collect(toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all quality rules", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all quality rules", ex);
        }
    }

    @Override
    public QualityRuleEntity create(ExecutionContext executionContext, NewQualityRuleEntity newEntity) {
        try {
            final QualityRule qualityRule = convert(newEntity);
            final QualityRule createdQualityRule = qualityRuleRepository.create(qualityRule);
            auditService.createEnvironmentAuditLog(
                executionContext,
                executionContext.getEnvironmentId(),
                Collections.singletonMap(QUALITY_RULE, createdQualityRule.getId()),
                QualityRule.AuditEvent.QUALITY_RULE_CREATED,
                qualityRule.getCreatedAt(),
                null,
                qualityRule
            );
            return convert(createdQualityRule);
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to create a quality rule {}", newEntity, e);
            throw new TechnicalManagementException("An error occurs while trying to create a quality rule " + newEntity, e);
        }
    }

    @Override
    public QualityRuleEntity update(ExecutionContext executionContext, UpdateQualityRuleEntity updateEntity) {
        try {
            final Optional<QualityRule> optionalQualityRule = qualityRuleRepository.findById(updateEntity.getId());
            if (!optionalQualityRule.isPresent()) {
                throw new QualityRuleNotFoundException(updateEntity.getId());
            }
            final QualityRule qualityRule = qualityRuleRepository.update(convert(updateEntity, optionalQualityRule.get()));
            auditService.createEnvironmentAuditLog(
                executionContext,
                executionContext.getEnvironmentId(),
                singletonMap(QUALITY_RULE, qualityRule.getId()),
                QUALITY_RULE_UPDATED,
                qualityRule.getUpdatedAt(),
                optionalQualityRule.get(),
                qualityRule
            );
            return convert(qualityRule);
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to update quality rule {}", updateEntity, e);
            throw new TechnicalManagementException("An error occurs while trying to update quality rule " + updateEntity, e);
        }
    }

    @Override
    public void delete(ExecutionContext executionContext, final String qualityRule) {
        try {
            final Optional<QualityRule> qualityRuleOptional = qualityRuleRepository.findById(qualityRule);
            if (qualityRuleOptional.isPresent()) {
                qualityRuleRepository.delete(qualityRule);
                // delete all reference on api quality rule
                apiQualityRuleRepository.deleteByQualityRule(qualityRule);
                auditService.createEnvironmentAuditLog(
                    executionContext,
                    executionContext.getEnvironmentId(),
                    Collections.singletonMap(QUALITY_RULE, qualityRule),
                    QualityRule.AuditEvent.QUALITY_RULE_DELETED,
                    new Date(),
                    null,
                    qualityRuleOptional.get()
                );
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete quality rule {}", qualityRule, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete quality rule " + qualityRule, ex);
        }
    }

    private QualityRuleEntity convert(QualityRule qualityRule) {
        QualityRuleEntity entity = new QualityRuleEntity();
        entity.setId(qualityRule.getId());
        entity.setName(qualityRule.getName());
        entity.setDescription(qualityRule.getDescription());
        entity.setWeight(qualityRule.getWeight());
        entity.setCreatedAt(qualityRule.getCreatedAt());
        entity.setUpdatedAt(qualityRule.getUpdatedAt());
        return entity;
    }

    private QualityRule convert(final NewQualityRuleEntity qualityRuleEntity) {
        final QualityRule qualityRule = new QualityRule();
        qualityRule.setId(UuidString.generateRandom());
        qualityRule.setName(qualityRuleEntity.getName());
        qualityRule.setDescription(qualityRuleEntity.getDescription());
        qualityRule.setWeight(qualityRuleEntity.getWeight());
        final Date now = new Date();
        qualityRule.setCreatedAt(now);
        qualityRule.setUpdatedAt(now);
        return qualityRule;
    }

    private QualityRule convert(final UpdateQualityRuleEntity qualityRuleEntity, final QualityRule qr) {
        final QualityRule qualityRule = new QualityRule();
        qualityRule.setId(qualityRuleEntity.getId());
        qualityRule.setName(qualityRuleEntity.getName());
        qualityRule.setDescription(qualityRuleEntity.getDescription());
        qualityRule.setWeight(qualityRuleEntity.getWeight());
        qualityRule.setCreatedAt(qr.getCreatedAt());
        qualityRule.setUpdatedAt(new Date());
        return qualityRule;
    }
}
