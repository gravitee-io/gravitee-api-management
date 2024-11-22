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
import io.gravitee.repository.management.model.ScoringFunction;
import java.util.Date;
import org.assertj.core.api.Condition;
import org.junit.Test;

public class ScoringFunctionRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/scoringfunction-tests/";
    }

    // create
    @Test
    public void create_should_create_function() throws TechnicalException {
        var date = new Date();
        var uuid = UUID.random().toString();
        var function = aFunction(uuid, date);

        var created = scoringFunctionRepository.create(function);

        assertThat(created).usingRecursiveComparison().isEqualTo(function);
    }

    @Test
    public void create_should_throw_when_creating_same_id() throws TechnicalException {
        var date = new Date();
        var uuid = UUID.random().toString();
        var function = aFunction(uuid, date);

        scoringFunctionRepository.create(function);
        assertThatThrownBy(() -> scoringFunctionRepository.create(function)).isInstanceOf(Exception.class);
    }

    // findById
    @Test
    public void findById_should_return_found_function() throws TechnicalException {
        var found = scoringFunctionRepository.findById("function1");

        assertThat(found).isPresent().get().extracting(ScoringFunction::getId).isEqualTo("function1");
    }

    @Test
    public void findById_should_return_empty_when_not_found() throws TechnicalException {
        var found = scoringFunctionRepository.findById("unknown");

        assertThat(found).isEmpty();
    }

    // findAllByReferenceId
    @Test
    public void findAllByReferenceId_should_return_found_functions() throws TechnicalException {
        var found = scoringFunctionRepository.findAllByReferenceId("my-env", "ENVIRONMENT");

        assertThat(found).hasSize(2).are(anyOf(haveId("function1"), haveId("function2")));
    }

    @Test
    public void findAllByReferenceId_should_return_empty_list_when_no_result() throws TechnicalException {
        var result = scoringFunctionRepository.findAllByReferenceId("unknown", "ENVIRONMENT");

        assertThat(result).isEmpty();
    }

    // delete
    @Test
    public void delete_should_delete_function() throws TechnicalException {
        var id = "to-delete";

        integrationRepository.delete(id);

        assertThat(integrationRepository.findById(id)).isEmpty();
    }

    // deleteByReferenceId
    @Test
    public void should_delete_by_reference_id() throws TechnicalException {
        int nbBeforeDeletion = scoringFunctionRepository.findAllByReferenceId("ToBeDeleted", "ENVIRONMENT").size();
        int deleted = scoringFunctionRepository.deleteByReferenceId("ToBeDeleted", "ENVIRONMENT").size();
        int nbAfterDeletion = scoringFunctionRepository.findAllByReferenceId("ToBeDeleted", "ENVIRONMENT").size();

        assertThat(nbBeforeDeletion).isEqualTo(2);
        assertThat(deleted).isEqualTo(2);
        assertThat(nbAfterDeletion).isZero();
    }

    private static ScoringFunction aFunction(String uuid, Date date) {
        return ScoringFunction
            .builder()
            .id(uuid)
            .name("function-name")
            .referenceType("ENVIRONMENT")
            .referenceId("my-env")
            .createdAt(date)
            .payload("function-payload")
            .build();
    }

    Condition<ScoringFunction> haveId(String id) {
        return new Condition<>(e -> id.equals(e.getId()), "have the id " + id);
    }
}
