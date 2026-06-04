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
package io.gravitee.apim.core.api.model.factory;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiModelFactoryTest {

    private static final String ENVIRONMENT_ID = "environment-id";

    @Test
    void fromCrd_should_preserve_flowExecution_when_specified() {
        var flowExecution = new FlowExecution();
        flowExecution.setMode(FlowMode.BEST_MATCH);
        flowExecution.setMatchRequired(true);
        var crd = minimalProxyCrd().toBuilder().flowExecution(flowExecution).build();

        Api api = ApiModelFactory.fromCrd(crd, ENVIRONMENT_ID);

        assertThat(api.getApiDefinitionHttpV4().getFlowExecution()).isEqualTo(flowExecution);
    }

    @Test
    void fromCrd_should_default_flowExecution_when_omitted() {
        var crd = minimalProxyCrd();

        Api api = ApiModelFactory.fromCrd(crd, ENVIRONMENT_ID);

        assertThat(api.getApiDefinitionHttpV4().getFlowExecution()).isEqualTo(new FlowExecution());
    }

    private static ApiCRDSpec minimalProxyCrd() {
        return ApiCRDSpec.builder()
            .id("api-id")
            .name("api")
            .version("1.0.0")
            .type("PROXY")
            .lifecycleState("CREATED")
            .state("STARTED")
            .visibility("PRIVATE")
            .definitionContext(DefinitionContext.builder().origin("KUBERNETES").syncFrom("KUBERNETES").build())
            .listeners(
                List.of(
                    HttpListener.builder()
                        .paths(List.of(Path.builder().path("/").build()))
                        .entrypoints(List.of(Entrypoint.builder().type("http-proxy").configuration("{}").build()))
                        .build()
                )
            )
            .endpointGroups(
                List.of(
                    EndpointGroup.builder()
                        .name("default")
                        .type("http-proxy")
                        .endpoints(List.of(Endpoint.builder().name("default").type("http-proxy").configuration("{}").build()))
                        .build()
                )
            )
            .properties(List.of())
            .flows(List.of())
            .plans(Map.of())
            .build();
    }
}
