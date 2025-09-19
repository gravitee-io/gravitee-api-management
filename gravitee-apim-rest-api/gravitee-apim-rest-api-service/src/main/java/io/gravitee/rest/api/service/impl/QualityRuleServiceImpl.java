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
import io.gravitee.rest.api.model.quality.QualityRuleReferenceType;
import io.gravitee.rest.api.model.quality.UpdateQualityRuleEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.QualityRuleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class QualityRuleServiceImpl extends AbstractService implements QualityRuleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QualityRuleServiceImpl.class);

    @Lazy
    @Autowired
    private QualityRuleRepository qualityRuleRepository;

    @Lazy
    @Autowired
    private ApiQualityRuleRepository apiQualityRuleRepository;

    @Autowired
    private AuditService auditService;

    @Override
    public QualityRuleEntity findByReferenceAndId(QualityRuleReferenceType referenceType, String referenceId, String id) {
        try {
            LOGGER.debug("Find quality rule by id : {}", id);
            return qualityRuleRepository
                .findById(id)
                .filter(
                    qr ->
                        qr.getReferenceType() == QualityRule.ReferenceType.valueOf(referenceType.name()) &&
                        qr.getReferenceId().equalsIgnoreCase(referenceId)
                )
                .map(this::convert)
                .orElseThrow(() -> new QualityRuleNotFoundException(id));
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
    public List<QualityRuleEntity> findByReference(QualityRuleReferenceType referenceType, String referenceId) {
        try {
            LOGGER.debug("Find quality rules for {} [{}]", referenceType, referenceId);
            return qualityRuleRepository
                .findByReference(repoQualityRuleReferenceType(referenceType), referenceId)
                .stream()
                .map(this::convert)
                .toList();
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find quality rules for {} [{}]", referenceType, referenceId, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to find quality rules for reference" + referenceType + " " + referenceId,
                ex
            );
        }
    }

    @Override
    public QualityRuleEntity create(
        ExecutionContext executionContext,
        NewQualityRuleEntity newEntity,
        QualityRuleReferenceType referenceType,
        String referenceId
    ) {
        try {
            final QualityRule qualityRule = convert(newEntity, referenceType, referenceId);
            final QualityRule createdQualityRule = qualityRuleRepository.create(qualityRule);
            auditService.createAuditLog(
                executionContext,
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
            final QualityRule qualityRule = qualityRuleRepository
                .findById(updateEntity.getId())
                .filter(
                    qr ->
                        qr.getReferenceType() == QualityRule.ReferenceType.ENVIRONMENT &&
                        qr.getReferenceId().equalsIgnoreCase(executionContext.getEnvironmentId())
                )
                .orElseThrow(() -> new QualityRuleNotFoundException(updateEntity.getId()));
            final QualityRule updatedQualityRule = qualityRuleRepository.update(convert(updateEntity, qualityRule));
            auditService.createAuditLog(
                executionContext,
                singletonMap(QUALITY_RULE, updatedQualityRule.getId()),
                QUALITY_RULE_UPDATED,
                updatedQualityRule.getUpdatedAt(),
                qualityRule,
                updatedQualityRule
            );
            return convert(updatedQualityRule);
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to update quality rule {}", updateEntity, e);
            throw new TechnicalManagementException("An error occurs while trying to update quality rule " + updateEntity, e);
        }
    }

    @Override
    public void delete(ExecutionContext executionContext, final String qualityRule) {
        try {
            final Optional<QualityRule> qualityRuleOptional = qualityRuleRepository
                .findById(qualityRule)
                .filter(
                    qr ->
                        qr.getReferenceType() == QualityRule.ReferenceType.ENVIRONMENT &&
                        qr.getReferenceId().equalsIgnoreCase(executionContext.getEnvironmentId())
                );
            if (qualityRuleOptional.isPresent()) {
                qualityRuleRepository.delete(qualityRule);
                // delete all reference on api quality rule
                apiQualityRuleRepository.deleteByQualityRule(qualityRule);
                auditService.createAuditLog(
                    executionContext,
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
        return QualityRuleEntity.builder()
            .id(qualityRule.getId())
            .referenceType(QualityRuleReferenceType.valueOf(qualityRule.getReferenceType().name()))
            .referenceId(qualityRule.getReferenceId())
            .name(qualityRule.getName())
            .description(qualityRule.getDescription())
            .weight(qualityRule.getWeight())
            .createdAt(qualityRule.getCreatedAt())
            .updatedAt(qualityRule.getUpdatedAt())
            .build();
    }

    private QualityRule convert(final NewQualityRuleEntity qualityRuleEntity, QualityRuleReferenceType referenceType, String referenceId) {
        final Date now = new Date();
        return QualityRule.builder()
            .id(UuidString.generateRandom())
            .referenceType(repoQualityRuleReferenceType(referenceType))
            .referenceId(referenceId)
            .name(qualityRuleEntity.getName())
            .description(qualityRuleEntity.getDescription())
            .weight(qualityRuleEntity.getWeight())
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private QualityRule convert(final UpdateQualityRuleEntity qualityRuleEntity, final QualityRule qr) {
        return QualityRule.builder()
            .id(qualityRuleEntity.getId())
            .referenceType(qr.getReferenceType())
            .referenceId(qr.getReferenceId())
            .name(qualityRuleEntity.getName())
            .description(qualityRuleEntity.getDescription())
            .weight(qualityRuleEntity.getWeight())
            .createdAt(qr.getCreatedAt())
            .updatedAt(new Date())
            .build();
    }

    private io.gravitee.repository.management.model.QualityRule.ReferenceType repoQualityRuleReferenceType(
        QualityRuleReferenceType referenceType
    ) {
        return io.gravitee.repository.management.model.QualityRule.ReferenceType.valueOf(referenceType.name());
    }
}
