/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(GraviteeContext.class)
public class ApiServiceCockpitImplTest {

    private static final String API_ID = "api#id";
    private static final String USER_ID = "user#id";
    private static final String ENVIRONMENT_ID = "environment#id";
    private static final String SWAGGER_DEFINITION = "";

    @Mock
    private ApiService apiService;

    @Mock
    private SwaggerService swaggerService;

    private ApiServiceCockpitImpl service;

    @Captor
    private ArgumentCaptor<ImportSwaggerDescriptorEntity> descriptorCaptor;

    @Captor
    private ArgumentCaptor<ObjectNode> apiDefinitionCaptor;

    @Before
    public void setUp() throws Exception {
        service = new ApiServiceCockpitImpl(new ObjectMapper(), apiService, swaggerService);
        PowerMockito.spy(GraviteeContext.class);
    }

    @Test
    public void createFromCockpit() {
        ImportSwaggerDescriptorEntity expectedDescriptor = new ImportSwaggerDescriptorEntity();
        expectedDescriptor.setPayload(SWAGGER_DEFINITION);
        expectedDescriptor.setWithDocumentation(true);
        expectedDescriptor.setWithPolicyPaths(true);
        expectedDescriptor.setWithPolicies(List.of("mock"));

        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);
        when(apiService.createWithApiDefinition(eq(swaggerApi), eq(USER_ID), any(ObjectNode.class))).thenReturn(api);

        when(apiService.exists(API_ID)).thenReturn(false);

        service.createOrUpdateFromCockpit(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID);

        PowerMockito.verifyStatic(GraviteeContext.class);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        verify(swaggerService).createAPI(descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService).createWithApiDefinition(eq(swaggerApi), eq(USER_ID), apiDefinitionCaptor.capture());
        assertThat(apiDefinitionCaptor.getValue().get("id")).isEqualTo(new JsonNodeFactory(false).textNode(API_ID));

        verify(apiService).createSystemFolder(API_ID);
        verify(apiService).createOrUpdateDocumentation(any(ImportSwaggerDescriptorEntity.class), eq(api), eq(true));
        verify(apiService).createMetadata(same(swaggerApi.getMetadata()), eq(API_ID));
    }

    @Test
    public void shouldUpdateApiFromCockpit() {
        ImportSwaggerDescriptorEntity expectedDescriptor = new ImportSwaggerDescriptorEntity();
        expectedDescriptor.setPayload(SWAGGER_DEFINITION);
        expectedDescriptor.setWithDocumentation(true);
        expectedDescriptor.setWithPolicyPaths(true);
        expectedDescriptor.setWithPolicies(List.of("mock"));

        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);

        when(apiService.exists(API_ID)).thenReturn(true);

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        when(apiService.updateFromSwagger(eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class)))
            .thenReturn(updatedApiEntity);

        final var result = service.createOrUpdateFromCockpit(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID);

        PowerMockito.verifyStatic(GraviteeContext.class);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        verify(swaggerService).createAPI(descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService, times(0)).createWithApiDefinition(any(UpdateApiEntity.class), anyString(), any(ObjectNode.class));
        verify(apiService, times(1)).updateFromSwagger(eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class));
        assertThat(result).isEqualTo(updatedApiEntity);
    }
}
