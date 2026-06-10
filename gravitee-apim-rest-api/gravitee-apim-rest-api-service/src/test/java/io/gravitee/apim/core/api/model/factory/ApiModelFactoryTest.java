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
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
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
    void fromApiExport_should_leave_allowedInApiProducts_null_for_v4_http_proxy_export_when_flag_missing() {
        ApiExport apiExport = ApiExport.builder()
            .name("my-api")
            .apiVersion("1.0.0")
            .definitionVersion(DefinitionVersion.V4)
            .type(ApiType.PROXY)
            // no allowedInApiProducts explicitly set
            .build();

        Api api = ApiModelFactory.fromApiExport(apiExport, ENVIRONMENT_ID);

        var definition = ((io.gravitee.definition.model.v4.Api) api.getApiDefinitionValue());
        assertThat(definition.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(definition.getType()).isEqualTo(ApiType.PROXY);
        assertThat(definition.getAllowedInApiProducts()).isNull();
    }

    @Test
    void fromApiExport_should_keep_true_when_allowedInApiProducts_true_in_export() {
        ApiExport apiExport = ApiExport.builder()
            .name("my-api")
            .apiVersion("1.0.0")
            .definitionVersion(DefinitionVersion.V4)
            .type(ApiType.PROXY)
            .allowedInApiProducts(true)
            .build();

        Api api = ApiModelFactory.fromApiExport(apiExport, ENVIRONMENT_ID);

        var definition = ((io.gravitee.definition.model.v4.Api) api.getApiDefinitionValue());
        assertThat(definition.getAllowedInApiProducts()).isTrue();
    }

    @Test
    void fromApiExport_should_keep_false_when_allowedInApiProducts_false_in_export() {
        ApiExport apiExport = ApiExport.builder()
            .name("my-api")
            .apiVersion("1.0.0")
            .definitionVersion(DefinitionVersion.V4)
            .type(ApiType.PROXY)
            .allowedInApiProducts(false)
            .build();

        Api api = ApiModelFactory.fromApiExport(apiExport, ENVIRONMENT_ID);

        var definition = ((io.gravitee.definition.model.v4.Api) api.getApiDefinitionValue());
        assertThat(definition.getAllowedInApiProducts()).isFalse();
    }

    @Test
    void fromApiExport_should_not_set_allowedInApiProducts_for_non_proxy_http_types() {
        // Given a V4 HTTP API of type MESSAGE
        ApiExport apiExport = ApiExport.builder()
            .name("my-api")
            .apiVersion("1.0.0")
            .definitionVersion(DefinitionVersion.V4)
            .type(ApiType.MESSAGE)
            .build();

        Api api = ApiModelFactory.fromApiExport(apiExport, ENVIRONMENT_ID);

        var definition = ((io.gravitee.definition.model.v4.Api) api.getApiDefinitionValue());
        assertThat(definition.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(definition.getType()).isEqualTo(ApiType.MESSAGE);
        assertThat(definition.getAllowedInApiProducts()).isNull();
    }

    @Test
    void fromApiExport_should_not_set_allowedInApiProducts_when_definition_version_is_not_v4() {
        // Given a non‑V4 PROXY API export
        ApiExport apiExport = ApiExport.builder()
            .name("my-api")
            .apiVersion("1.0.0")
            .definitionVersion(DefinitionVersion.V2)
            .type(ApiType.PROXY)
            .build();

        Api api = ApiModelFactory.fromApiExport(apiExport, ENVIRONMENT_ID);

        var definition = ((io.gravitee.definition.model.v4.Api) api.getApiDefinitionValue());
        assertThat(definition.getType()).isEqualTo(ApiType.PROXY);
        assertThat(definition.getAllowedInApiProducts()).isNull();
    }

    @Test
    void fromCrd_should_forward_allowedInApiProducts_and_allowMultiJwtOauth2Subscriptions() {
        var crd = minimalProxyCrd(true, true);

        Api api = ApiModelFactory.fromCrd(crd, ENVIRONMENT_ID);

        assertThat(api.isAllowMultiJwtOauth2Subscriptions()).isTrue();
        assertThat(api.getApiDefinitionHttpV4().getAllowedInApiProducts()).isTrue();
    }

    @Test
    void fromCrd_should_preserve_flowExecution_when_specified() {
        var flowExecution = new FlowExecution();
        flowExecution.setMode(FlowMode.BEST_MATCH);
        flowExecution.setMatchRequired(true);
        var crd = minimalProxyCrd(true, true).toBuilder().flowExecution(flowExecution).build();

        Api api = ApiModelFactory.fromCrd(crd, ENVIRONMENT_ID);

        assertThat(api.getApiDefinitionHttpV4().getFlowExecution()).isEqualTo(flowExecution);
    }

    @Test
    void fromCrd_should_default_flowExecution_when_omitted() {
        var crd = minimalProxyCrd(true, true);

        Api api = ApiModelFactory.fromCrd(crd, ENVIRONMENT_ID);

        assertThat(api.getApiDefinitionHttpV4().getFlowExecution()).isEqualTo(new FlowExecution());
    }

    private static ApiCRDSpec minimalProxyCrd(boolean allowedInApiProducts, boolean allowMultiJwtOauth2Subscriptions) {
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
            .allowedInApiProducts(allowedInApiProducts)
            .allowMultiJwtOauth2Subscriptions(allowMultiJwtOauth2Subscriptions)
            .build();
    }
}
