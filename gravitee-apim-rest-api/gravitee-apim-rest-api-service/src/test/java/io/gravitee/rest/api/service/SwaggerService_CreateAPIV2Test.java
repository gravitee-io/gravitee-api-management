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
package io.gravitee.rest.api.service;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SwaggerService_CreateAPIV2Test extends SwaggerService_CreateAPITest {

    protected DefinitionVersion getDefinitionVersion() {
        return DefinitionVersion.V2;
    }

    protected SwaggerApiEntity createAPI(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        return swaggerService.createAPI(swaggerDescriptor, this.getDefinitionVersion());
    }

    @Override
    protected void validate(SwaggerApiEntity api, String expectedEndpoint, String expectedContextPath) {
        validateApi(api, expectedEndpoint, expectedContextPath);
        validatePolicies(api, 2, 4, asList("/pets", "/pets/:petId"));
    }

    @Override
    protected void validatePolicies(SwaggerApiEntity api, int expectedPathSize, int expectedFlowSize, List<String> expectedPaths) {
        assertEquals(expectedFlowSize, api.getFlows().size());
        List<String> paths = api.getFlows().stream().map(flow -> flow.getPath()).collect(Collectors.toList());
        assertTrue(paths.containsAll(expectedPaths));
    }

    @Override
    protected void validateRules(
        SwaggerApiEntity api,
        String path,
        int expectedRuleSize,
        List<HttpMethod> firstRuleMethods,
        String firstRuleDescription
    ) {
        List<Flow> flows = api.getFlows().stream().filter(flow1 -> flow1.getPath().equals(path)).collect(Collectors.toList());
        //        assertEquals(1, flows.size());
        assertEquals(expectedRuleSize, flows.get(0).getPre().size());
        assertTrue(flows.get(0).getMethods().containsAll(firstRuleMethods));
        Step step = flows.get(0).getPre().get(0);
        assertNotNull(step);
        assertEquals(firstRuleDescription, step.getDescription());
    }
}
