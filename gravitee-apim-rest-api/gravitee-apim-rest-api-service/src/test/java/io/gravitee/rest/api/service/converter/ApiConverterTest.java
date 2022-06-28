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
package io.gravitee.rest.api.service.converter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiConverterTest {

    @InjectMocks
    private ApiConverter apiConverter;

    @Mock
    ObjectMapper objectMapper;

    @Test
    public void toUpdateApiEntity_should_keep_crossId() {
        ApiEntity apiEntity = buildTestApiEntity();
        apiEntity.setCrossId("test-cross-id");

        UpdateApiEntity updateApiEntity = apiConverter.toUpdateApiEntity(apiEntity);

        assertEquals("test-cross-id", updateApiEntity.getCrossId());
    }

    @Test
    public void toUpdateApiEntity_should_reset_crossId_if_param_set_to_true() {
        ApiEntity apiEntity = buildTestApiEntity();
        apiEntity.setCrossId("test-cross-id");

        UpdateApiEntity updateApiEntity = apiConverter.toUpdateApiEntity(apiEntity, true);

        assertNull("test-cross-id", updateApiEntity.getCrossId());
    }

    @Test
    public void toApiEntity_should_get_flows_from_api_definition() throws JsonProcessingException {
        Api api = new Api();
        api.setDefinition("my-api-definition");

        io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
        apiDefinition.setProxy(new Proxy());
        apiDefinition.setFlows(List.of(new Flow(), new Flow()));
        when(objectMapper.readValue("my-api-definition", io.gravitee.definition.model.Api.class)).thenReturn(apiDefinition);

        ApiEntity apiEntity = apiConverter.toApiEntity(api, null);

        assertNotNull(apiEntity.getFlows());
        assertEquals(2, apiEntity.getFlows().size());
    }

    @Test
    public void toApiEntity_should_set_empty_flows_list_if_no_flows_in_definition() throws JsonProcessingException {
        Api api = new Api();
        api.setDefinition("my-api-definition");

        io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
        apiDefinition.setProxy(new Proxy());
        apiDefinition.setFlows(null);
        when(objectMapper.readValue("my-api-definition", io.gravitee.definition.model.Api.class)).thenReturn(apiDefinition);

        ApiEntity apiEntity = apiConverter.toApiEntity(api, null);

        assertNotNull(apiEntity.getFlows());
        assertEquals(0, apiEntity.getFlows().size());
    }

    private ApiEntity buildTestApiEntity() {
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        return apiEntity;
    }
}
