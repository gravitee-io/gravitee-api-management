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
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.management.model.ApiQualityRule;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApiQualityRuleRepositoryTest extends AbstractManagementRepositoryTest {

    public static final Date DATE = new Date(1439022010883L);
    public static final Date NEW_DATE = new Date(1546305346000L);

    @Override
    protected String getTestCasesPath() {
        return "/data/qualityrule-tests/";
    }

    @Test
    public void shouldFindByApi() throws Exception {
        final List<ApiQualityRule> apiQualityRules = apiQualityRuleRepository.findByApi("api2");

        assertNotNull(apiQualityRules);
        assertEquals(1, apiQualityRules.size());
        final ApiQualityRule apiQualityRuleProduct = apiQualityRules
            .stream()
            .filter(apiQualityRule -> "quality-rule1".equals(apiQualityRule.getQualityRule()))
            .findAny()
            .get();
        assertTrue(apiQualityRuleProduct.isChecked());
        assertTrue(compareDate(DATE, apiQualityRuleProduct.getCreatedAt()));
        assertTrue(compareDate(DATE, apiQualityRuleProduct.getUpdatedAt()));
    }

    @Test
    public void shouldCreate() throws Exception {
        final ApiQualityRule apiQualityRule = new ApiQualityRule();
        apiQualityRule.setApi("api2");
        apiQualityRule.setQualityRule("new-apiQualityRule");
        apiQualityRule.setChecked(true);
        apiQualityRule.setCreatedAt(DATE);
        apiQualityRule.setUpdatedAt(DATE);

        int nbApiQualityRulesBeforeCreation = apiQualityRuleRepository.findByApi("api2").size();
        apiQualityRuleRepository.create(apiQualityRule);
        int nbApiQualityRulesAfterCreation = apiQualityRuleRepository.findByApi("api2").size();

        Assertions.assertEquals(nbApiQualityRulesBeforeCreation + 1, nbApiQualityRulesAfterCreation);

        Optional<ApiQualityRule> optional = apiQualityRuleRepository.findById("api2", "new-apiQualityRule");
        Assertions.assertTrue(optional.isPresent(), "ApiQualityRule saved not found");

        final ApiQualityRule apiQualityRuleSaved = optional.get();
        Assertions.assertEquals(apiQualityRule.isChecked(), apiQualityRuleSaved.isChecked(), "Invalid saved apiQualityRule checked.");
        Assertions.assertTrue(compareDate(apiQualityRule.getCreatedAt(), apiQualityRuleSaved.getCreatedAt()), "Invalid createdAt.");
        Assertions.assertTrue(compareDate(apiQualityRule.getUpdatedAt(), apiQualityRuleSaved.getUpdatedAt()), "Invalid updatedAt.");
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<ApiQualityRule> optional = apiQualityRuleRepository.findById("api2", "quality-rule1");
        Assertions.assertTrue(optional.isPresent(), "ApiQualityRule to update not found");
        assertTrue(optional.get().isChecked(), "Invalid saved apiQualityRule checked.");

        final ApiQualityRule apiQualityRule = optional.get();
        apiQualityRule.setChecked(false);
        apiQualityRule.setCreatedAt(NEW_DATE);
        apiQualityRule.setUpdatedAt(NEW_DATE);

        int nbApiQualityRulesBeforeUpdate = apiQualityRuleRepository.findByApi("api2").size();
        apiQualityRuleRepository.update(apiQualityRule);
        int nbApiQualityRulesAfterUpdate = apiQualityRuleRepository.findByApi("api2").size();

        Assertions.assertEquals(nbApiQualityRulesBeforeUpdate, nbApiQualityRulesAfterUpdate);

        Optional<ApiQualityRule> optionalUpdated = apiQualityRuleRepository.findById("api2", "quality-rule1");
        Assertions.assertTrue(optionalUpdated.isPresent(), "ApiQualityRule to update not found");

        final ApiQualityRule apiQualityRuleUpdated = optionalUpdated.get();
        assertFalse(apiQualityRuleUpdated.isChecked(), "Invalid apiQualityRule checked.");
        Assertions.assertTrue(compareDate(NEW_DATE, apiQualityRuleUpdated.getCreatedAt()), "Invalid createdAt.");
        Assertions.assertTrue(compareDate(NEW_DATE, apiQualityRuleUpdated.getUpdatedAt()), "Invalid updatedAt.");
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbApiQualityRulesBeforeDeletion = apiQualityRuleRepository.findByApi("api1").size();
        apiQualityRuleRepository.delete("api1", "quality-rule2");
        int nbApiQualityRulesAfterDeletion = apiQualityRuleRepository.findByApi("api1").size();

        Assertions.assertEquals(nbApiQualityRulesBeforeDeletion - 1, nbApiQualityRulesAfterDeletion);
    }

    @Test
    public void shouldDeleteByApi() throws Exception {
        int nbApiQualityRulesBeforeDeletion = apiQualityRuleRepository.findByApi("api1").size();
        Assertions.assertNotEquals(0, nbApiQualityRulesBeforeDeletion);

        apiQualityRuleRepository.deleteByApi("api1");
        int nbApiQualityRulesAfterDeletion = apiQualityRuleRepository.findByApi("api1").size();

        Assertions.assertEquals(0, nbApiQualityRulesAfterDeletion);
    }

    @Test
    public void shouldDeleteByQualityRule() throws Exception {
        int nbApiQualityRulesBeforeDeletion = apiQualityRuleRepository.findByQualityRule("quality-rule2").size();
        Assertions.assertNotEquals(0, nbApiQualityRulesBeforeDeletion);

        apiQualityRuleRepository.deleteByQualityRule("quality-rule2");
        int nbApiQualityRulesAfterDeletion = apiQualityRuleRepository.findByQualityRule("quality-rule2").size();

        Assertions.assertEquals(0, nbApiQualityRulesAfterDeletion);
    }

    @Test
    public void shouldNotUpdateUnknownApiQualityRule() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            ApiQualityRule unknownApiQualityRule = new ApiQualityRule();
            unknownApiQualityRule.setApi("unknown");
            unknownApiQualityRule.setQualityRule("unknown");
            apiQualityRuleRepository.update(unknownApiQualityRule);
            fail("An unknown apiQualityRule should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            apiQualityRuleRepository.update(null);
            fail("A null apiQualityRule should not be updated");
        });
    }
}
