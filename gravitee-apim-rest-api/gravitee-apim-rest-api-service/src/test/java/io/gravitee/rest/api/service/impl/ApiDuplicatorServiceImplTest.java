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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import java.util.Collections;
import org.junit.BeforeClass;
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

    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void beforeClass() {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        mapper.setFilterProvider(new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter)));
    }

    @Test
    public void handleApiDefinitionIds_should_regenerate_api_id_and_plans_id_on_another_environment() {
        ObjectNode apiDefinition = mapper.createObjectNode().put("id", "my-api-1").put("environment_id", "dev");

        apiDefinition.set(
            "plans",
            mapper
                .createArrayNode()
                .add(mapper.createObjectNode().put("id", "my-plan-id-1"))
                .add(mapper.createObjectNode().put("id", "my-plan-id-2"))
        );

        JsonNode newApiDefinition = apiDuplicatorService.handleApiDefinitionIds(apiDefinition, "uat");

        assertEquals("e0a6482a-b8a7-3db4-a1b7-d36a462a9e38", newApiDefinition.get("id").asText());
        assertEquals("393ed51c-285d-3097-82eb-2bff2903dc62", newApiDefinition.get("plans").get(0).get("id").asText());
        assertEquals("bff87514-39d4-331b-a531-73c021ecf627", newApiDefinition.get("plans").get(1).get("id").asText());
    }

    @Test
    public void handleApiDefinitionIds_should_not_regenerate_api_id_and_plans_id_on_the_same_environment() {
        ObjectNode apiDefinition = mapper.createObjectNode().put("id", "my-api-1").put("environment_id", "dev");

        apiDefinition.set(
            "plans",
            mapper
                .createArrayNode()
                .add(mapper.createObjectNode().put("id", "my-plan-id-1"))
                .add(mapper.createObjectNode().put("id", "my-plan-id-2"))
        );

        JsonNode newApiDefinition = apiDuplicatorService.handleApiDefinitionIds(apiDefinition, "dev");

        assertEquals("my-api-1", newApiDefinition.get("id").asText());
        assertEquals("my-plan-id-1", newApiDefinition.get("plans").get(0).get("id").asText());
        assertEquals("my-plan-id-2", newApiDefinition.get("plans").get(1).get("id").asText());
    }
}
