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
package io.gravitee.apim.rest.api.mcp.core.use_case;

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.MCP_CALLER;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.NOW;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.PROD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListFilterValuesUseCaseTest {

    private final Fakes.StubAnalyticsQueries analytics = new Fakes.StubAnalyticsQueries();
    private final ListFilterValuesUseCase useCase = new ListFilterValuesUseCase(Fakes.environments(PROD), Fakes.apis(), analytics);

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
    }

    @Test
    void should_list_the_ids_a_filter_takes_with_their_names() {
        var output = useCase.execute(new ListFilterValuesUseCase.Input(MCP_CALLER, null, "application", "mob", "2h", null, 5));

        assertThat(output.values())
            .extracting(ListFilterValuesUseCase.Value::value, ListFilterValuesUseCase.Value::id)
            .containsExactly(org.assertj.core.groups.Tuple.tuple("Mobile app", "app-id"));
        assertThat(output.total()).isEqualTo(1);
        assertThat(output.filter()).isEqualTo("APPLICATION");
        var window = TimeWindow.parse("2h", null, NOW);
        assertThat(analytics.filterValuesQuery).isEqualTo("APPLICATION|" + window.from() + "|" + window.to() + "|5|mob");
    }

    @Test
    void should_name_the_catalog_when_the_filter_is_unknown() {
        assertThatThrownBy(() -> useCase.execute(new ListFilterValuesUseCase.Input(MCP_CALLER, null, "country", null, null, null, null)))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("Unknown filter 'country'");
    }
}
