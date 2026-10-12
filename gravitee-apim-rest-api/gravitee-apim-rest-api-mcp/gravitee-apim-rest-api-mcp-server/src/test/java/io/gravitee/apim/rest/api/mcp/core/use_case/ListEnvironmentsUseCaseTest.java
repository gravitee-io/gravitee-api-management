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
import io.gravitee.apim.rest.api.mcp.core.model.EnvRef;
import io.gravitee.apim.rest.api.mcp.core.use_case.ListEnvironmentsUseCase.Environment;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ListEnvironmentsUseCaseTest {

    private final Fakes.Directory environments = Fakes.environments(PROD, STAGING);
    private final ListEnvironmentsUseCase useCase = new ListEnvironmentsUseCase(
        environments,
        Fakes.apisByEnvironment(
            Map.of(PROD.id(), List.of(new ApiRef("orders", "Orders"), new ApiRef("stock", "Stock")), STAGING.id(), List.of())
        )
    );

    @Test
    void should_list_readable_environments_first_with_how_many_apis_the_caller_sees() {
        environments.unreadable.add(new EnvRef("e-dev", null, "Dev"));

        var output = useCase.execute(new ListEnvironmentsUseCase.Input(MCP_CALLER));

        assertThat(output.environments())
            .extracting(Environment::hrid, Environment::name, Environment::readable, Environment::visibleApis)
            .containsExactly(tuple("prod", "Production", true, 2), tuple("staging", "Staging", true, 0), tuple(null, "Dev", false, null));
        assertThat(output.summary()).isEqualTo(
            "2 environment(s) you can read, 1 you cannot. Pass environment (id, hrid or name) to the tools that work on one."
        );
    }

    @Test
    void should_say_that_a_single_readable_environment_needs_no_argument() {
        environments.readable.remove(STAGING);

        assertThat(useCase.execute(new ListEnvironmentsUseCase.Input(MCP_CALLER)).summary()).endsWith(
            "Pass environment only when a tool asks for it."
        );
    }
}
