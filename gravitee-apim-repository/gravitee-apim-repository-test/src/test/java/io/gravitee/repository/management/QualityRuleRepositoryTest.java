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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.Assert.*;

import io.gravitee.repository.management.model.QualityRule;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
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

        assertNotNull(qualityRules);
        assertEquals(3, qualityRules.size());
        final QualityRule qualityRuleProduct = qualityRules
            .stream()
            .filter(qualityRule -> "quality-rule3".equals(qualityRule.getId()))
            .findAny()
            .get();
        assertEquals("Api-key plan", qualityRuleProduct.getName());
        assertEquals("A plan api-key is published", qualityRuleProduct.getDescription());
        assertEquals(3, qualityRuleProduct.getWeight());
        assertTrue(compareDate(DATE, qualityRuleProduct.getCreatedAt()));
        assertTrue(compareDate(DATE, qualityRuleProduct.getUpdatedAt()));
    }

    @Test
    public void shouldCreate() throws Exception {
        final QualityRule qualityRule = new QualityRule();
        qualityRule.setId("new-qualityRule");
        qualityRule.setName("QualityRule name");
        qualityRule.setDescription("QualityRule description");
        qualityRule.setWeight(10);
        qualityRule.setCreatedAt(DATE);
        qualityRule.setUpdatedAt(DATE);

        int nbQualityRulesBeforeCreation = qualityRuleRepository.findAll().size();
        qualityRuleRepository.create(qualityRule);
        int nbQualityRulesAfterCreation = qualityRuleRepository.findAll().size();

        Assert.assertEquals(nbQualityRulesBeforeCreation + 1, nbQualityRulesAfterCreation);

        Optional<QualityRule> optional = qualityRuleRepository.findById("new-qualityRule");
        Assert.assertTrue("QualityRule saved not found", optional.isPresent());

        final QualityRule qualityRuleSaved = optional.get();
        Assert.assertEquals("Invalid saved qualityRule name.", qualityRule.getName(), qualityRuleSaved.getName());
        Assert.assertEquals("Invalid saved qualityRule description.", qualityRule.getDescription(), qualityRuleSaved.getDescription());
        Assert.assertEquals("Invalid weight.", qualityRule.getWeight(), qualityRuleSaved.getWeight());
        Assert.assertTrue("Invalid createdAt.", compareDate(qualityRule.getCreatedAt(), qualityRuleSaved.getCreatedAt()));
        Assert.assertTrue("Invalid updatedAt.", compareDate(qualityRule.getUpdatedAt(), qualityRuleSaved.getUpdatedAt()));
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<QualityRule> optional = qualityRuleRepository.findById("quality-rule1");
        Assert.assertTrue("QualityRule to update not found", optional.isPresent());
        Assert.assertEquals("Invalid saved qualityRule name.", "Description in english", optional.get().getName());
        Assert.assertEquals("Invalid saved qualityRule name.", "Description must be in english", optional.get().getDescription());

        final QualityRule qualityRule = optional.get();
        qualityRule.setName("New name");
        qualityRule.setDescription("New description");
        qualityRule.setWeight(5);
        qualityRule.setCreatedAt(DATE);
        qualityRule.setUpdatedAt(DATE);

        int nbQualityRulesBeforeUpdate = qualityRuleRepository.findAll().size();
        qualityRuleRepository.update(qualityRule);
        int nbQualityRulesAfterUpdate = qualityRuleRepository.findAll().size();

        Assert.assertEquals(nbQualityRulesBeforeUpdate, nbQualityRulesAfterUpdate);

        Optional<QualityRule> optionalUpdated = qualityRuleRepository.findById("quality-rule1");
        Assert.assertTrue("QualityRule to update not found", optionalUpdated.isPresent());

        final QualityRule qualityRuleUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved qualityRule name.", "New name", qualityRuleUpdated.getName());
        Assert.assertEquals("Invalid saved qualityRule description.", "New description", qualityRuleUpdated.getDescription());
        Assert.assertEquals("Invalid weight.", 5, qualityRuleUpdated.getWeight());
        Assert.assertTrue("Invalid createdAt.", compareDate(DATE, qualityRuleUpdated.getCreatedAt()));
        Assert.assertTrue("Invalid updatedAt.", compareDate(DATE, qualityRuleUpdated.getUpdatedAt()));
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbQualityRulesBeforeDeletion = qualityRuleRepository.findAll().size();
        qualityRuleRepository.delete("quality-rule2");
        int nbQualityRulesAfterDeletion = qualityRuleRepository.findAll().size();

        Assert.assertEquals(nbQualityRulesBeforeDeletion - 1, nbQualityRulesAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownQualityRule() throws Exception {
        QualityRule unknownQualityRule = new QualityRule();
        unknownQualityRule.setId("unknown");
        qualityRuleRepository.update(unknownQualityRule);
        fail("An unknown qualityRule should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        qualityRuleRepository.update(null);
        fail("A null qualityRule should not be updated");
    }
}
