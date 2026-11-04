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
package io.gravitee.apim.core.guardian.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.GuardianFixtures;
import inmemory.GuardianQueryServiceInMemory;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchGuardianUseCaseTest {

    private final GuardianQueryServiceInMemory guardianQueryService = new GuardianQueryServiceInMemory();
    private SearchGuardianUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchGuardianUseCase(guardianQueryService);
        guardianQueryService.initWith(
            IntStream.range(0, 12)
                .mapToObj(i -> GuardianFixtures.aGuardian().id("id-" + i).name(String.format("guardian %02d", i)).build())
                .toList()
        );
    }

    @AfterEach
    void tearDown() {
        guardianQueryService.reset();
    }

    @Test
    void should_page_the_results() {
        var output = useCase.execute(new SearchGuardianUseCase.Input("env-id", null, new PageableImpl(2, 5), "name"));

        assertThat(output.guardians().getTotalElements()).isEqualTo(12);
        assertThat(output.guardians().getContent()).hasSize(5);
        assertThat(output.guardians().getContent().get(0).getName()).isEqualTo("guardian 05");
    }

    @Test
    void should_filter_by_name() {
        var output = useCase.execute(new SearchGuardianUseCase.Input("env-id", "guardian 0", new PageableImpl(1, 20), null));

        assertThat(output.guardians().getTotalElements()).isEqualTo(10);
    }

    @Test
    void should_return_nothing_for_another_environment() {
        var output = useCase.execute(new SearchGuardianUseCase.Input("other-env", null, new PageableImpl(1, 10), null));

        assertThat(output.guardians().getContent()).isEmpty();
    }

    @Test
    void should_refuse_an_unknown_sort_field() {
        var input = new SearchGuardianUseCase.Input("env-id", null, new PageableImpl(1, 10), "definition");

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("definition");
    }
}
