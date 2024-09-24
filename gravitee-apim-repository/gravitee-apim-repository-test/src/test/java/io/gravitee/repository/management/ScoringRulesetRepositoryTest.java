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

import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Integration;
import io.gravitee.repository.management.model.ScoringRuleset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.Condition;
import org.junit.Test;

public class ScoringRulesetRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/scoringruleset-tests/";
    }

    // create
    @Test
    public void create_should_create_ruleset() throws TechnicalException {
        var date = new Date();
        var uuid = UUID.random().toString();
        var ruleset = aRuleset(uuid, date);

        var created = scoringRulesetRepository.create(ruleset);

        assertThat(created).usingRecursiveComparison().isEqualTo(ruleset);
    }

    @Test
    public void create_should_throw_when_creating_same_id() throws TechnicalException {
        var date = new Date();
        var uuid = UUID.random().toString();
        var ruleset = aRuleset(uuid, date);

        scoringRulesetRepository.create(ruleset);
        assertThatThrownBy(() -> scoringRulesetRepository.create(ruleset)).isInstanceOf(Exception.class);
    }

    // findById
    @Test
    public void findById_should_return_found_ruleset() throws TechnicalException {
        var found = scoringRulesetRepository.findById("ruleset1");

        assertThat(found).isPresent().get().extracting(ScoringRuleset::getId).isEqualTo("ruleset1");
    }

    @Test
    public void findById_should_return_empty_when_not_found() throws TechnicalException {
        var found = scoringRulesetRepository.findById("unknown");

        assertThat(found).isEmpty();
    }

    // findAllByReferenceId
    @Test
    public void findAllByReferenceId_should_return_found_rulesets() throws TechnicalException {
        var found = scoringRulesetRepository.findAllByReferenceId("my-env", "ENVIRONMENT");

        assertThat(found).hasSize(2).are(anyOf(haveId("ruleset1"), haveId("ruleset2")));
    }

    @Test
    public void findAllByReferenceId_should_return_empty_list_when_no_result() throws TechnicalException {
        var result = scoringRulesetRepository.findAllByReferenceId("unknown", "ENVIRONMENT");

        assertThat(result).isEmpty();
    }

    // delete
    @Test
    public void delete_should_delete_ruleset() throws TechnicalException {
        var id = "to-delete";

        integrationRepository.delete(id);

        assertThat(integrationRepository.findById(id)).isEmpty();
    }

    // deleteByReferenceId
    @Test
    public void should_delete_by_reference_id() throws TechnicalException {
        int nbBeforeDeletion = scoringRulesetRepository.findAllByReferenceId("ToBeDeleted", "ENVIRONMENT").size();
        int deleted = scoringRulesetRepository.deleteByReferenceId("ToBeDeleted", "ENVIRONMENT").size();
        int nbAfterDeletion = scoringRulesetRepository.findAllByReferenceId("ToBeDeleted", "ENVIRONMENT").size();

        assertThat(nbBeforeDeletion).isEqualTo(2);
        assertThat(deleted).isEqualTo(2);
        assertThat(nbAfterDeletion).isZero();
    }

    private static ScoringRuleset aRuleset(String uuid, Date date) {
        return ScoringRuleset
            .builder()
            .id(uuid)
            .name("ruleset-name")
            .description("ruleset-description")
            .referenceType("ENVIRONMENT")
            .referenceId("my-env")
            .createdAt(date)
            .payload("ruleset-payload")
            .build();
    }

    Condition<ScoringRuleset> haveId(String id) {
        return new Condition<>(e -> id.equals(e.getId()), "have the id " + id);
    }
}
