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

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiDuplicatorServiceImplTest {

    @InjectMocks
    protected ApiDuplicatorServiceImpl apiDuplicatorService;

    @Test
    public void preprocessApiDefinitionUpdatingIds_should_regenerate_plans_id() throws JsonProcessingException {
        JsonNode jsonNode = new ObjectMapper()
        .readTree("{\"id\":\"my-api-id\",\"plans\":[{\"id\":\"my-plan-id1\"},{\"id\":\"my-plan-id2\"}]}");

        String newApiDefinition = apiDuplicatorService.preprocessApiDefinitionUpdatingIds(jsonNode, "default");

        // plans id have been regenerated in jsonNodes and stringified api definition
        assertEquals("b53f77ba-380a-34e2-8c5d-5c60847f0de4", jsonNode.get("plans").get(0).get("id").asText());
        assertEquals("1b552fa9-bb70-3bbc-b925-9e0b6dd4a35d", jsonNode.get("plans").get(1).get("id").asText());
        assertEquals(
            "{\"id\":\"my-api-id\",\"plans\":[{\"id\":\"b53f77ba-380a-34e2-8c5d-5c60847f0de4\"},{\"id\":\"1b552fa9-bb70-3bbc-b925-9e0b6dd4a35d\"}]}",
            newApiDefinition
        );
    }
}
