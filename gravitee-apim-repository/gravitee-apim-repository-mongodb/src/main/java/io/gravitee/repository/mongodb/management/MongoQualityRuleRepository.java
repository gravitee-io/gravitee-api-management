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

import static java.util.stream.Collectors.toSet;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.QualityRuleRepository;
import io.gravitee.repository.management.model.QualityRule;
import io.gravitee.repository.mongodb.management.internal.model.QualityRuleMongo;
import io.gravitee.repository.mongodb.management.internal.quality.QualityRuleMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoQualityRuleRepository implements QualityRuleRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoQualityRuleRepository.class);

    @Autowired
    private QualityRuleMongoRepository internalQualityRuleRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<QualityRule> findById(String qualityRuleId) throws TechnicalException {
        LOGGER.debug("Find quality rule by ID [{}]", qualityRuleId);

        final QualityRuleMongo qualityRule = internalQualityRuleRepo.findById(qualityRuleId).orElse(null);

        LOGGER.debug("Find quality rule by ID [{}] - Done", qualityRuleId);
        return Optional.ofNullable(mapper.map(qualityRule));
    }

    @Override
    public QualityRule create(QualityRule qualityRule) throws TechnicalException {
        LOGGER.debug("Create quality rule [{}]", qualityRule.getName());

        QualityRuleMongo qualityRuleMongo = mapper.map(qualityRule);
        QualityRuleMongo createdQualityRuleMongo = internalQualityRuleRepo.insert(qualityRuleMongo);

        QualityRule res = mapper.map(createdQualityRuleMongo);

        LOGGER.debug("Create quality rule [{}] - Done", qualityRule.getName());

        return res;
    }

    @Override
    public QualityRule update(QualityRule qualityRule) throws TechnicalException {
        if (qualityRule == null || qualityRule.getName() == null) {
            throw new IllegalStateException("QualityRule to update must have a name");
        }

        final QualityRuleMongo qualityRuleMongo = internalQualityRuleRepo.findById(qualityRule.getId()).orElse(null);

        if (qualityRuleMongo == null) {
            throw new IllegalStateException(String.format("No quality rule found with name [%s]", qualityRule.getId()));
        }

        try {
            qualityRuleMongo.setName(qualityRule.getName());
            qualityRuleMongo.setReferenceType(qualityRule.getReferenceType().name());
            qualityRuleMongo.setReferenceId(qualityRule.getReferenceId());
            qualityRuleMongo.setDescription(qualityRule.getDescription());
            qualityRuleMongo.setWeight(qualityRule.getWeight());
            qualityRuleMongo.setCreatedAt(qualityRule.getCreatedAt());
            qualityRuleMongo.setUpdatedAt(qualityRule.getUpdatedAt());

            QualityRuleMongo qualityRuleMongoUpdated = internalQualityRuleRepo.save(qualityRuleMongo);
            return mapper.map(qualityRuleMongoUpdated);
        } catch (Exception e) {
            final String error = "An error occurred when updating quality rule";
            LOGGER.error(error, e);
            throw new TechnicalException(error);
        }
    }

    @Override
    public void delete(String qualityRuleId) throws TechnicalException {
        try {
            internalQualityRuleRepo.deleteById(qualityRuleId);
        } catch (Exception e) {
            final String error = "An error occurred when deleting quality rule " + qualityRuleId;
            LOGGER.error(error, e);
            throw new TechnicalException(error);
        }
    }

    @Override
    public Set<QualityRule> findAll() {
        final List<QualityRuleMongo> qualityRules = internalQualityRuleRepo.findAll();
        return qualityRules
            .stream()
            .map(qualityRuleMongo -> mapper.map(qualityRuleMongo))
            .collect(toSet());
    }

    @Override
    public List<QualityRule> findByReference(QualityRule.ReferenceType referenceType, String referenceId) throws TechnicalException {
        try {
            return internalQualityRuleRepo
                .findByReference(referenceType.name(), referenceId)
                .stream()
                .map(qualityRuleMongo -> mapper.map(qualityRuleMongo))
                .toList();
        } catch (Exception e) {
            final String error =
                "An error occurred when finding all quality rules with findByReference " + referenceType + " [" + referenceId + "]";
            LOGGER.error(error, e);
            throw new TechnicalException(error);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(final String referenceId, final QualityRule.ReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("Delete quality rules by reference [{}, {}]", referenceType, referenceId);
        try {
            List<QualityRuleMongo> qualityRuleMongos = internalQualityRuleRepo.deleteByReferenceIdAndReferenceType(
                referenceId,
                referenceType.name()
            );
            LOGGER.debug("Delete quality rules by reference [{}, {}] - Done", referenceType, referenceId);
            return qualityRuleMongos.stream().map(QualityRuleMongo::getId).toList();
        } catch (Exception e) {
            throw new TechnicalException("An error occurred while deleting quality rules", e);
        }
    }
}
