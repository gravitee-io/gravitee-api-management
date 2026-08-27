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
package io.gravitee.apim.infra.domain_service.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import inmemory.GroupQueryServiceInMemory;
import inmemory.PolicyPluginCrudServiceInMemory;
import inmemory.TagQueryServiceInMemory;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.plugin.domain_service.EndpointConnectorPluginDomainService;
import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.service.exceptions.SwaggerDescriptorException;
import io.gravitee.rest.api.service.impl.swagger.policy.impl.PolicyOperationVisitorManagerImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OAIDomainServiceImplTest {

    private static final String ORGANIZATION_ID = "organizationId";
    private static final String ENVIRONMENT_ID = "environmentId";

    private OAIDomainServiceImpl oaiDomainService;
    private final PolicyOperationVisitorManagerImpl policyOperationVisitorManager = new PolicyOperationVisitorManagerImpl();

    @BeforeEach
    void setUp() {
        oaiDomainService = new OAIDomainServiceImpl(policyOperationVisitorManager, null, null, null, null);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void should_throw_exception_when_specification_is_null(String payload) {
        // Given
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        importSwaggerDescriptor.setPayload(payload);

        // When
        assertThatThrownBy(() -> oaiDomainService.convert(ORGANIZATION_ID, ENVIRONMENT_ID, importSwaggerDescriptor, false, false))
            .isExactlyInstanceOf(SwaggerDescriptorException.class)
            .hasMessage("Payload cannot be null");
    }

    @Test
    void should_throw_exception_when_specification_is_does_not_contains_info_section() {
        // Given
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        importSwaggerDescriptor.setPayload("{ \"openapi\": \"3.0.0\" }");

        // When
        assertThatThrownBy(() ->
            oaiDomainService.convert(ORGANIZATION_ID, ENVIRONMENT_ID, importSwaggerDescriptor, false, false)
        ).isExactlyInstanceOf(SwaggerDescriptorException.class);
    }

    @Nested
    @WireMockTest
    class RemoteUrlPayload {

        private static final String REMOTE_SPEC = """
            {
              "openapi": "3.0.3",
              "info": { "title": "Remote", "version": "1.0.0" },
              "servers": [ { "url": "https://api.example.com/remote" } ],
              "paths": {
                "/pets": {
                  "get": {
                    "operationId": "listPets",
                    "responses": {
                      "200": {
                        "description": "ok",
                        "content": {
                          "application/json": {
                            "schema": { "$ref": "#/components/schemas/Pet" }
                          }
                        }
                      }
                    }
                  }
                }
              },
              "components": {
                "schemas": {
                  "Pet": { "type": "object", "properties": { "name": { "type": "string" } } }
                }
              }
            }
            """;

        private OAIDomainServiceImpl service;

        @BeforeEach
        void setUp() {
            var policyPluginCrudService = new PolicyPluginCrudServiceInMemory();
            policyPluginCrudService.initWith(List.of(PolicyPlugin.builder().id("oas-validation").name("OAS Validation").build()));
            var endpointConnectorPluginService = mock(EndpointConnectorPluginDomainService.class);
            when(endpointConnectorPluginService.getDefaultSharedConfiguration(anyString())).thenReturn("{}");

            service = new OAIDomainServiceImpl(
                new PolicyOperationVisitorManagerImpl(),
                new GroupQueryServiceInMemory(),
                new TagQueryServiceInMemory(),
                endpointConnectorPluginService,
                policyPluginCrudService
            );
        }

        private String stubSpecUrl(WireMockRuntimeInfo wm) {
            wm.getWireMock().register(get(urlEqualTo("/openapi.json")).willReturn(aResponse().withStatus(200).withBody(REMOTE_SPEC)));
            return wm.getHttpBaseUrl() + "/openapi.json";
        }

        @Test
        void should_store_the_fetched_specification_as_documentation_rather_than_the_url(WireMockRuntimeInfo wm) {
            var url = stubSpecUrl(wm);
            var descriptor = ImportSwaggerDescriptorEntity.builder().payload(url).build();

            var result = service.convert(ORGANIZATION_ID, ENVIRONMENT_ID, descriptor, true, false);

            assertThat(result).isNotNull();
            assertThat(result.getPages()).hasSize(1);
            var page = result.getPages().getFirst();
            assertThat(page.getType()).isEqualTo(Page.Type.SWAGGER);
            assertThat(page.getContent()).isNotEqualTo(url).contains("\"openapi\"").contains("listPets");
        }

        @Test
        void should_give_the_oas_validation_policy_the_fetched_specification_rather_than_the_url(WireMockRuntimeInfo wm) {
            var url = stubSpecUrl(wm);
            var descriptor = ImportSwaggerDescriptorEntity.builder().payload(url).build();

            var result = service.convert(ORGANIZATION_ID, ENVIRONMENT_ID, descriptor, false, true);

            assertThat(result).isNotNull();
            assertThat(result.getApiExport().getResources()).hasSize(1);
            var configuration = result.getApiExport().getResources().getFirst().getConfiguration();
            assertThat(configuration).isNotNull();
            assertThat(configuration.toString()).doesNotContain(url).contains("listPets");
        }

        @Test
        void should_keep_an_inline_payload_verbatim(WireMockRuntimeInfo wm) {
            var descriptor = ImportSwaggerDescriptorEntity.builder().payload(REMOTE_SPEC).build();

            var result = service.convert(ORGANIZATION_ID, ENVIRONMENT_ID, descriptor, true, false);

            assertThat(result).isNotNull();
            assertThat(result.getPages().getFirst().getContent()).isEqualTo(REMOTE_SPEC);
        }
    }
}
