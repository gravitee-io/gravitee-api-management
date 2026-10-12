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
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.PROD;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.STAGING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ListApisUseCaseTest {

    private final ListApisUseCase useCase = new ListApisUseCase(
        Fakes.environments(PROD, STAGING),
        Fakes.apisByEnvironment(
            Map.of(
                PROD.id(),
                List.of(new ApiRef("orders", "Orders"), new ApiRef("stock", "Stock")),
                STAGING.id(),
                List.of(new ApiRef("stock-staging", "Stock"))
            )
        )
    );

    @Test
    void should_list_the_apis_of_every_readable_environment_with_their_handles() {
        var output = useCase.execute(new ListApisUseCase.Input(MCP_CALLER, null, null));

        assertThat(output.apis())
            .extracting(entry -> entry.environment().hrid(), entry -> entry.api().id(), entry -> entry.api().type())
            .containsExactly(tuple("prod", "orders", "PROXY"), tuple("prod", "stock", "PROXY"), tuple("staging", "stock-staging", "PROXY"));
        assertThat(output.summary()).isEqualTo("3 API(s).");
    }

    @Test
    void should_narrow_to_an_environment_and_a_name() {
        var output = useCase.execute(new ListApisUseCase.Input(MCP_CALLER, "staging", "STO"));

        assertThat(output.apis())
            .extracting(entry -> entry.api().id())
            .containsExactly("stock-staging");
        assertThat(output.summary()).isEqualTo("1 API(s) named like 'sto'.");
    }
}
