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
package io.gravitee.apim.core.api.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.FlowCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.OAIDomainService;
import io.gravitee.apim.core.api.exception.InvalidApiDefinitionException;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OAIToUpdateApiUseCaseTest {

    private static final String API_ID = "my-api";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo("org-id", "env-id", "user-id");

    private OAIDomainService oaiDomainService;
    private FlowCrudServiceInMemory flowCrudService;
    private UpdateApiDefinitionUseCase updateApiDefinitionUseCase;
    private OAIToUpdateApiUseCase cut;

    @BeforeEach
    void setUp() {
        oaiDomainService = mock(OAIDomainService.class);
        flowCrudService = new FlowCrudServiceInMemory();
        updateApiDefinitionUseCase = mock(UpdateApiDefinitionUseCase.class);
        cut = new OAIToUpdateApiUseCase(oaiDomainService, flowCrudService, updateApiDefinitionUseCase);
    }

    @Test
    void should_throw_when_oai_conversion_returns_null() {
        when(oaiDomainService.convert(anyString(), anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(null);

        assertThatThrownBy(() ->
            cut.execute(
                OAIToUpdateApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .importSwaggerDescriptor(new ImportSwaggerDescriptorEntity())
                    .auditInfo(AUDIT_INFO)
                    .build()
            )
        )
            .isInstanceOf(InvalidApiDefinitionException.class)
            .hasMessage("Unable to read the swagger specification");
    }

    @Test
    void should_reuse_persisted_http_flow_id_when_swagger_import_matches_path_and_methods() {
        var persistedSelector = HttpSelector.builder().pathOperator(Operator.EQUALS).path("/pets").methods(Set.of(HttpMethod.GET)).build();
        var persistedFlow = Flow.builder().id("persisted-flow-id").selectors(List.of(persistedSelector)).build();
        flowCrudService.saveApiFlows(API_ID, List.of(persistedFlow));

        var importSelector = HttpSelector.builder().pathOperator(Operator.EQUALS).path("/pets").methods(Set.of(HttpMethod.GET)).build();
        var importedFlow = Flow.builder().id(null).selectors(List.of(importSelector)).build();
        var importDefinition = ImportDefinition.builder()
            .apiExport(ApiExport.builder().definitionVersion(DefinitionVersion.V4).flows(List.of(importedFlow)).build())
            .build();

        when(oaiDomainService.convert(anyString(), anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(importDefinition);

        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        when(updateApiDefinitionUseCase.execute(any())).thenReturn(
            new UpdateApiDefinitionUseCase.Output(new ApiWithFlows(existingApi, List.of()))
        );

        cut.execute(
            OAIToUpdateApiUseCase.Input.builder()
                .apiId(API_ID)
                .importSwaggerDescriptor(new ImportSwaggerDescriptorEntity())
                .withPolicyPaths(false)
                .auditInfo(AUDIT_INFO)
                .build()
        );

        var inputCaptor = ArgumentCaptor.forClass(UpdateApiDefinitionUseCase.Input.class);
        org.mockito.Mockito.verify(updateApiDefinitionUseCase).execute(inputCaptor.capture());
        var flowsPassedToUseCase = inputCaptor.getValue().importDefinition().getApiExport().getFlows();
        assertThat(flowsPassedToUseCase).hasSize(1);
        assertThat(flowsPassedToUseCase.getFirst().getId()).isEqualTo("persisted-flow-id");
    }

    @Test
    void should_not_set_flow_id_when_no_persisted_flow_matches_swagger_import() {
        var importSelector = HttpSelector.builder().pathOperator(Operator.EQUALS).path("/unknown").methods(Set.of(HttpMethod.POST)).build();
        var importedFlow = Flow.builder().id(null).selectors(List.of(importSelector)).build();
        var importDefinition = ImportDefinition.builder()
            .apiExport(ApiExport.builder().definitionVersion(DefinitionVersion.V4).flows(List.of(importedFlow)).build())
            .build();

        when(oaiDomainService.convert(anyString(), anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(importDefinition);

        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        when(updateApiDefinitionUseCase.execute(any())).thenReturn(
            new UpdateApiDefinitionUseCase.Output(new ApiWithFlows(existingApi, List.of()))
        );

        cut.execute(
            OAIToUpdateApiUseCase.Input.builder()
                .apiId(API_ID)
                .importSwaggerDescriptor(new ImportSwaggerDescriptorEntity())
                .withPolicyPaths(false)
                .auditInfo(AUDIT_INFO)
                .build()
        );

        var inputCaptor = ArgumentCaptor.forClass(UpdateApiDefinitionUseCase.Input.class);
        org.mockito.Mockito.verify(updateApiDefinitionUseCase).execute(inputCaptor.capture());
        var flowsPassedToUseCase = inputCaptor.getValue().importDefinition().getApiExport().getFlows();
        assertThat(flowsPassedToUseCase).hasSize(1);
        assertThat(flowsPassedToUseCase.getFirst().getId()).isNull();
    }

    @Test
    void should_assign_persisted_flow_id_only_to_first_incoming_flow_when_two_incoming_flows_share_same_key() {
        var persistedSelector = HttpSelector.builder().pathOperator(Operator.EQUALS).path("/pets").methods(Set.of(HttpMethod.GET)).build();
        var persistedFlow = Flow.builder().id("persisted-flow-id").selectors(List.of(persistedSelector)).build();
        flowCrudService.saveApiFlows(API_ID, List.of(persistedFlow));

        var selector = HttpSelector.builder().pathOperator(Operator.EQUALS).path("/pets").methods(Set.of(HttpMethod.GET)).build();
        var firstIncomingFlow = Flow.builder().id(null).selectors(List.of(selector)).build();
        var secondIncomingFlow = Flow.builder().id(null).selectors(List.of(selector)).build();
        var importDefinition = ImportDefinition.builder()
            .apiExport(
                ApiExport.builder().definitionVersion(DefinitionVersion.V4).flows(List.of(firstIncomingFlow, secondIncomingFlow)).build()
            )
            .build();

        when(oaiDomainService.convert(anyString(), anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(importDefinition);

        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        when(updateApiDefinitionUseCase.execute(any())).thenReturn(
            new UpdateApiDefinitionUseCase.Output(new ApiWithFlows(existingApi, List.of()))
        );

        cut.execute(
            OAIToUpdateApiUseCase.Input.builder()
                .apiId(API_ID)
                .importSwaggerDescriptor(new ImportSwaggerDescriptorEntity())
                .withPolicyPaths(false)
                .auditInfo(AUDIT_INFO)
                .build()
        );

        var inputCaptor = ArgumentCaptor.forClass(UpdateApiDefinitionUseCase.Input.class);
        org.mockito.Mockito.verify(updateApiDefinitionUseCase).execute(inputCaptor.capture());
        var flowsPassedToUseCase = inputCaptor.getValue().importDefinition().getApiExport().getFlows();
        assertThat(flowsPassedToUseCase).hasSize(2);
        // first flow claims the persisted ID; second flow gets null and will be created as new
        assertThat(flowsPassedToUseCase.get(0).getId()).isEqualTo("persisted-flow-id");
        assertThat(flowsPassedToUseCase.get(1).getId()).isNull();
    }

    @Test
    void should_assign_first_persisted_flow_id_when_two_persisted_flows_share_same_key() {
        var selector = HttpSelector.builder().pathOperator(Operator.EQUALS).path("/pets").methods(Set.of(HttpMethod.GET)).build();
        var firstPersistedFlow = Flow.builder().id("persisted-flow-id-1").selectors(List.of(selector)).build();
        var secondPersistedFlow = Flow.builder().id("persisted-flow-id-2").selectors(List.of(selector)).build();
        flowCrudService.saveApiFlows(API_ID, List.of(firstPersistedFlow, secondPersistedFlow));

        var importedFlow = Flow.builder().id(null).selectors(List.of(selector)).build();
        var importDefinition = ImportDefinition.builder()
            .apiExport(ApiExport.builder().definitionVersion(DefinitionVersion.V4).flows(List.of(importedFlow)).build())
            .build();

        when(oaiDomainService.convert(anyString(), anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(importDefinition);

        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        when(updateApiDefinitionUseCase.execute(any())).thenReturn(
            new UpdateApiDefinitionUseCase.Output(new ApiWithFlows(existingApi, List.of()))
        );

        cut.execute(
            OAIToUpdateApiUseCase.Input.builder()
                .apiId(API_ID)
                .importSwaggerDescriptor(new ImportSwaggerDescriptorEntity())
                .withPolicyPaths(false)
                .auditInfo(AUDIT_INFO)
                .build()
        );

        var inputCaptor = ArgumentCaptor.forClass(UpdateApiDefinitionUseCase.Input.class);
        org.mockito.Mockito.verify(updateApiDefinitionUseCase).execute(inputCaptor.capture());
        var flowsPassedToUseCase = inputCaptor.getValue().importDefinition().getApiExport().getFlows();
        assertThat(flowsPassedToUseCase).hasSize(1);
        // merge function keeps first persisted ID on duplicate key
        assertThat(flowsPassedToUseCase.getFirst().getId()).isEqualTo("persisted-flow-id-1");
    }

    @Test
    void should_not_reuse_persisted_flow_id_when_with_policy_paths_is_true() {
        var persistedSelector = HttpSelector.builder().pathOperator(Operator.EQUALS).path("/pets").methods(Set.of(HttpMethod.GET)).build();
        var persistedFlow = Flow.builder().id("persisted-flow-id").selectors(List.of(persistedSelector)).build();
        flowCrudService.saveApiFlows(API_ID, List.of(persistedFlow));

        var importSelector = HttpSelector.builder().pathOperator(Operator.EQUALS).path("/pets").methods(Set.of(HttpMethod.GET)).build();
        var importedFlow = Flow.builder().id(null).selectors(List.of(importSelector)).build();
        var importDefinition = ImportDefinition.builder()
            .apiExport(ApiExport.builder().definitionVersion(DefinitionVersion.V4).flows(List.of(importedFlow)).build())
            .build();

        when(oaiDomainService.convert(anyString(), anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(importDefinition);

        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        when(updateApiDefinitionUseCase.execute(any())).thenReturn(
            new UpdateApiDefinitionUseCase.Output(new ApiWithFlows(existingApi, List.of()))
        );

        cut.execute(
            OAIToUpdateApiUseCase.Input.builder()
                .apiId(API_ID)
                .importSwaggerDescriptor(new ImportSwaggerDescriptorEntity())
                .withPolicyPaths(true)
                .auditInfo(AUDIT_INFO)
                .build()
        );

        var inputCaptor = ArgumentCaptor.forClass(UpdateApiDefinitionUseCase.Input.class);
        org.mockito.Mockito.verify(updateApiDefinitionUseCase).execute(inputCaptor.capture());
        var flowsPassedToUseCase = inputCaptor.getValue().importDefinition().getApiExport().getFlows();
        assertThat(flowsPassedToUseCase).hasSize(1);
        assertThat(flowsPassedToUseCase.getFirst().getId()).isNull();
    }
}
