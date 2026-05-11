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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static fixtures.core.model.ApiFixtures.aProxyApiV4;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PolicyPluginCrudServiceInMemory;
import inmemory.TagQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.import_definition.ImportDefinitionUpdateDomainServiceTestInitializer;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plugin.domain_service.EndpointConnectorPluginDomainService;
import io.gravitee.apim.infra.domain_service.api.OAIDomainServiceImpl;
import io.gravitee.apim.infra.domain_service.api.WsdlParserDomainServiceImpl;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.exceptions.SwaggerDescriptorException;
import io.gravitee.rest.api.service.impl.swagger.policy.impl.PolicyOperationVisitorManagerImpl;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WsdlToUpdateApiUseCaseIntegrationTest {

    private static final String API_ID = "existing-api-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final String SHARED_CONFIGURATION = """
            { "description": "shared config" }
        """;
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    private final EndpointConnectorPluginDomainService endpointConnectorPluginService = mock(EndpointConnectorPluginDomainService.class);
    private final PolicyPluginCrudServiceInMemory policyPluginCrudService = new PolicyPluginCrudServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();

    private WsdlToUpdateApiUseCase useCase;
    private ImportDefinitionUpdateDomainServiceTestInitializer updateInitializer;

    @BeforeEach
    void setUp() {
        updateInitializer = new ImportDefinitionUpdateDomainServiceTestInitializer(apiCrudService);

        when(endpointConnectorPluginService.getDefaultSharedConfiguration(anyString())).thenReturn(SHARED_CONFIGURATION);

        when(updateInitializer.validateApiDomainService.validateAndSanitizeForUpdate(any(), any(), any(), any(), any())).thenAnswer(inv ->
            inv.getArgument(1)
        );

        when(updateInitializer.apiService.update(any(), any(), any(), anyBoolean(), any())).thenAnswer(inv -> {
            io.gravitee.rest.api.model.v4.api.UpdateApiEntity updateApi = inv.getArgument(2);
            return ApiEntity.builder().id(API_ID).name(updateApi.getName()).apiVersion(updateApi.getApiVersion()).build();
        });

        ImportConfiguration importConfiguration = mock(ImportConfiguration.class);
        when(importConfiguration.isAllowImportFromPrivate()).thenReturn(true);
        when(importConfiguration.getImportWhitelist()).thenReturn(List.of());

        var oaiDomainService = new OAIDomainServiceImpl(
            new PolicyOperationVisitorManagerImpl(),
            new GroupQueryServiceInMemory(),
            new TagQueryServiceInMemory(),
            endpointConnectorPluginService,
            policyPluginCrudService
        );

        var updateApiDefinitionUseCase = new UpdateApiDefinitionFromImportUseCase(
            apiCrudService,
            updateInitializer.initialize(ENVIRONMENT_ID),
            flowCrudService
        );

        var oaiToUpdateApiUseCase = new OAIToUpdateApiUseCase(oaiDomainService, flowCrudService, updateApiDefinitionUseCase);

        useCase = new WsdlToUpdateApiUseCase(new WsdlParserDomainServiceImpl(importConfiguration), oaiToUpdateApiUseCase);
    }

    @AfterEach
    void tearDown() {
        updateInitializer.tearDown();
        Stream.of(apiCrudService, flowCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_preserve_api_id_when_updating_from_wsdl() {
        givenExistingApi(API_ID);

        var output = useCase.execute(buildInput(loadWsdl(), false, false));

        assertThat(output).isNotNull();
        assertThat(output.apiWithFlows().getId()).isEqualTo(API_ID);
    }

    @Test
    void should_update_api_name_from_wsdl_service_element() {
        givenExistingApi(API_ID);

        useCase.execute(buildInput(loadWsdl(), false, false));

        var captor = ArgumentCaptor.forClass(io.gravitee.rest.api.model.v4.api.UpdateApiEntity.class);
        verify(updateInitializer.apiService).update(any(), any(), captor.capture(), anyBoolean(), any());
        assertThat(captor.getValue().getName()).isEqualTo("CalculatorService");
    }

    @Test
    void should_have_no_flows_when_no_policies_are_requested() {
        givenExistingApi(API_ID);

        useCase.execute(buildInput(loadWsdl(), false, false));

        var captor = ArgumentCaptor.forClass(io.gravitee.rest.api.model.v4.api.UpdateApiEntity.class);
        verify(updateInitializer.apiService).update(any(), any(), captor.capture(), anyBoolean(), any());
        assertThat(captor.getValue().getFlows()).isEmpty();
    }

    @Test
    void should_update_endpoint_url_from_wsdl_soap_address() {
        givenExistingApi(API_ID);

        useCase.execute(buildInput(loadWsdl(), false, false));

        var captor = ArgumentCaptor.forClass(io.gravitee.rest.api.model.v4.api.UpdateApiEntity.class);
        verify(updateInitializer.apiService).update(any(), any(), captor.capture(), anyBoolean(), any());
        assertThat(captor.getValue().getEndpointGroups())
            .isNotEmpty()
            .first()
            .satisfies(endpointGroup ->
                assertThat(endpointGroup.getEndpoints())
                    .isNotEmpty()
                    .first()
                    .satisfies(endpoint -> assertThat(endpoint.getConfiguration()).contains("http://localhost:8080/calculator"))
            );
    }

    @Test
    void should_throw_when_wsdl_payload_is_invalid() {
        givenExistingApi(API_ID);

        var throwable = catchThrowable(() -> useCase.execute(buildInput("not valid wsdl content", false, false)));

        assertThat(throwable).isInstanceOf(SwaggerDescriptorException.class);
    }

    @Nested
    @WireMockTest
    class WithUrlUpdate {

        @Test
        void should_update_api_from_remote_wsdl_url(WireMockRuntimeInfo wm) {
            givenExistingApi(API_ID);
            var wsdlContent = loadWsdl();
            wm.getWireMock().register(get(urlEqualTo("/calculator.wsdl")).willReturn(aResponse().withStatus(200).withBody(wsdlContent)));

            var input = new WsdlToUpdateApiUseCase.Input.Url(
                API_ID,
                "http://localhost:" + wm.getHttpPort() + "/calculator.wsdl",
                false,
                false,
                List.of(),
                AUDIT_INFO
            );
            var output = useCase.execute(input);

            assertThat(output).isNotNull();
            assertThat(output.apiWithFlows().getId()).isEqualTo(API_ID);
        }
    }

    private void givenExistingApi(String apiId) {
        var existingApi = aProxyApiV4().toBuilder().id(apiId).environmentId(ENVIRONMENT_ID).build();
        apiCrudService.initWith(List.of(existingApi));
    }

    @SneakyThrows
    private String loadWsdl() {
        return Resources.toString(Resources.getResource("wsdl/calculator.wsdl"), Charsets.UTF_8);
    }

    private WsdlToUpdateApiUseCase.Input buildInput(String payload, boolean withDocumentation, boolean withOASValidationPolicy) {
        return new WsdlToUpdateApiUseCase.Input.Inline(API_ID, payload, withDocumentation, withOASValidationPolicy, List.of(), AUDIT_INFO);
    }
}
