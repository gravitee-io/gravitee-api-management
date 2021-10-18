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
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.SwaggerService;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiServiceCockpitImplTest {

    private static final String API_ID = "api#id";
    private static final String USER_ID = "user#id";
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

        service.createFromCockpit(API_ID, USER_ID, SWAGGER_DEFINITION);

        verify(swaggerService).createAPI(descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService).createWithApiDefinition(eq(swaggerApi), eq(USER_ID), apiDefinitionCaptor.capture());
        assertThat(apiDefinitionCaptor.getValue().get("id")).isEqualTo(new JsonNodeFactory(false).textNode(API_ID));

        verify(apiService).createSystemFolder(API_ID);
        verify(apiService).createOrUpdateDocumentation(any(ImportSwaggerDescriptorEntity.class), eq(api), eq(true));
        verify(apiService).createMetadata(same(swaggerApi.getMetadata()), eq(API_ID));
    }
}
