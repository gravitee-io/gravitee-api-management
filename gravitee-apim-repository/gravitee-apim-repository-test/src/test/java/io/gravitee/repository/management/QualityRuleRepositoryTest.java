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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.QualityRule;
import java.util.*;
import org.junit.Test;

public class QualityRuleRepositoryTest extends AbstractManagementRepositoryTest {

    public static final Date DATE = new Date(1439022010883L);

    @Override
    protected String getTestCasesPath() {
        return "/data/qualityrule-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<QualityRule> qualityRules = qualityRuleRepository.findAll();

        assertThat(qualityRules).isNotNull().hasSize(4);

        assertThat(qualityRules)
            .extracting(QualityRule::getId)
            .containsExactlyInAnyOrder("quality-rule1", "quality-rule2", "quality-rule3", "quality-rule-other-env");

        final QualityRule qualityRuleProduct = qualityRules
            .stream()
            .filter(qualityRule -> "quality-rule3".equals(qualityRule.getId()))
            .findAny()
            .get();
        assertThat(qualityRuleProduct.getName()).isEqualTo("Api-key plan");
        assertThat(qualityRuleProduct.getReferenceType()).isEqualTo(QualityRule.ReferenceType.ENVIRONMENT);
        assertThat(qualityRuleProduct.getReferenceId()).isEqualTo("b78f2219-890d-4344-8f22-19890d834442");
        assertThat(qualityRuleProduct.getDescription()).isEqualTo("A plan api-key is published");
        assertThat(qualityRuleProduct.getWeight()).isEqualTo(3);
        assertThat(compareDate(DATE, qualityRuleProduct.getCreatedAt())).isTrue();
        assertThat(compareDate(DATE, qualityRuleProduct.getUpdatedAt())).isTrue();
    }

    @Test
    public void shouldCreate() throws Exception {
        final QualityRule qualityRule = QualityRule.builder()
            .id("new-qualityRule")
            .referenceType(QualityRule.ReferenceType.ENVIRONMENT)
            .referenceId("4b78f2219-890d-4344-8f22-19890d83444")
            .name("QualityRule name")
            .description("QualityRule description")
            .weight(10)
            .createdAt(DATE)
            .updatedAt(DATE)
            .build();

        int nbQualityRulesBeforeCreation = qualityRuleRepository.findAll().size();
        qualityRuleRepository.create(qualityRule);
        int nbQualityRulesAfterCreation = qualityRuleRepository.findAll().size();
        assertThat(nbQualityRulesAfterCreation).isEqualTo(nbQualityRulesBeforeCreation + 1);

        Optional<QualityRule> optional = qualityRuleRepository.findById("new-qualityRule");
        assertThat(optional).hasValueSatisfying(savedQualityRule -> {
            assertThat(savedQualityRule.getName()).isEqualTo(qualityRule.getName());
            assertThat(savedQualityRule.getDescription()).isEqualTo(qualityRule.getDescription());
            assertThat(savedQualityRule.getWeight()).isEqualTo(qualityRule.getWeight());
            assertThat(savedQualityRule.getReferenceType()).isEqualTo(qualityRule.getReferenceType());
            assertThat(savedQualityRule.getReferenceId()).isEqualTo(qualityRule.getReferenceId());
            assertThat(compareDate(qualityRule.getCreatedAt(), savedQualityRule.getCreatedAt())).isTrue();
            assertThat(compareDate(qualityRule.getCreatedAt(), savedQualityRule.getUpdatedAt())).isTrue();
        });
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<QualityRule> optional = qualityRuleRepository.findById("quality-rule1");
        assertThat(optional).isPresent();
        assertThat(optional.get().getName()).isEqualTo("Description in english");
        assertThat(optional.get().getDescription()).isEqualTo("Description must be in english");

        final QualityRule qualityRule = optional.get();
        qualityRule.setName("New name");
        qualityRule.setDescription("New description");
        qualityRule.setWeight(5);
        qualityRule.setCreatedAt(DATE);
        qualityRule.setUpdatedAt(DATE);

        int nbQualityRulesBeforeUpdate = qualityRuleRepository.findAll().size();
        qualityRuleRepository.update(qualityRule);
        int nbQualityRulesAfterUpdate = qualityRuleRepository.findAll().size();
        assertThat(nbQualityRulesAfterUpdate).isEqualTo(nbQualityRulesBeforeUpdate);

        Optional<QualityRule> optionalUpdated = qualityRuleRepository.findById("quality-rule1");
        assertThat(optionalUpdated).hasValueSatisfying(updatedQualityRule -> {
            assertThat(updatedQualityRule.getName()).isEqualTo("New name");
            assertThat(updatedQualityRule.getDescription()).isEqualTo("New description");
            assertThat(updatedQualityRule.getWeight()).isEqualTo(5);
            assertThat(updatedQualityRule.getReferenceType()).isEqualTo(QualityRule.ReferenceType.ENVIRONMENT);
            assertThat(updatedQualityRule.getReferenceId()).isEqualTo("b78f2219-890d-4344-8f22-19890d834442");
            assertThat(compareDate(DATE, updatedQualityRule.getCreatedAt())).isTrue();
            assertThat(compareDate(DATE, updatedQualityRule.getUpdatedAt())).isTrue();
        });
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbQualityRulesBeforeDeletion = qualityRuleRepository.findAll().size();
        qualityRuleRepository.delete("quality-rule2");
        int nbQualityRulesAfterDeletion = qualityRuleRepository.findAll().size();

        assertThat(nbQualityRulesAfterDeletion).isEqualTo(nbQualityRulesBeforeDeletion - 1);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownQualityRule() throws Exception {
        QualityRule unknownQualityRule = new QualityRule();
        unknownQualityRule.setId("unknown");
        qualityRuleRepository.update(unknownQualityRule);

        qualityRuleRepository.update(unknownQualityRule);
        fail("An unknown qualityRule should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        qualityRuleRepository.update(null);
        fail("A null qualityRule should not be updated");
    }

    @Test
    public void shouldFindByReference() throws TechnicalException {
        final List<QualityRule> qualityRules = qualityRuleRepository.findByReference(
            QualityRule.ReferenceType.ENVIRONMENT,
            "b78f2219-890d-4344-8f22-19890d834442"
        );

        assertThat(qualityRules)
            .hasSize(3)
            .extracting(QualityRule::getId)
            .containsExactlyInAnyOrder("quality-rule1", "quality-rule2", "quality-rule3");
    }
}
