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
package io.gravitee.rest.api.service.impl.swagger.converter.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.policy.api.swagger.Policy;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.model.v4.policy.PolicyPluginEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitor;
import io.gravitee.rest.api.service.impl.swagger.policy.impl.PolicyOperationVisitorManagerImpl;
import io.gravitee.rest.api.service.impl.swagger.visitor.v3.OAIOperationVisitor;
import io.gravitee.rest.api.service.swagger.OAIDescriptor;
import io.gravitee.rest.api.service.v4.PolicyPluginService;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAIToAPIV2ConverterTest {

    private PolicyPluginService policyPluginService;
    private PolicyOperationVisitorManagerImpl policyOperationVisitorManager;
    private OAIToAPIV2Converter converter;

    @BeforeEach
    void setup() {
        policyPluginService = mock(PolicyPluginService.class);
        policyOperationVisitorManager = mock(PolicyOperationVisitorManagerImpl.class);
    }

    @Test
    @DisplayName("Should set plugin name and policyId correctly")
    void shouldSetMockPluginNameAndPolicyId() {
        ImportSwaggerDescriptorEntity descriptor = new ImportSwaggerDescriptorEntity();
        descriptor.setWithPolicyPaths(true);
        descriptor.setWithPathMapping(true);
        descriptor.setWithPolicies(List.of("json-validation"));

        OAIOperationVisitor visitor = mock(OAIOperationVisitor.class);
        Policy policy = new Policy();
        policy.setName("json-validation");
        policy.setConfiguration("{}");
        when(visitor.visit(any(OpenAPI.class), any(Operation.class))).thenReturn(Optional.of(policy));

        PolicyOperationVisitor policyVisitor = mock(PolicyOperationVisitor.class);
        when(policyVisitor.getId()).thenReturn("json-validation");

        when(policyOperationVisitorManager.getPolicyVisitors()).thenReturn(List.of(policyVisitor));
        when(policyOperationVisitorManager.getOAIOperationVisitor("json-validation")).thenReturn(visitor);

        PolicyPluginEntity plugin = new PolicyPluginEntity();
        plugin.setName("JSON Validation");
        when(policyPluginService.findById("json-validation")).thenReturn(plugin);

        converter = new OAIToAPIV2Converter(
            descriptor,
            policyOperationVisitorManager,
            mock(GroupService.class),
            mock(TagService.class),
            policyPluginService
        );

        OpenAPI openAPI = new OpenAPI()
            .info(new Info().title("Mock API").version("1.0"))
            .paths(new Paths().addPathItem("/pets", new PathItem().get(new Operation().operationId("listPets"))))
            .servers(List.of());

        SwaggerApiEntity api = converter.convert(new ExecutionContext("DEFAULT", "DEFAULT"), new OAIDescriptor(openAPI));

        assertThat(api).isNotNull();
        assertThat(api.getFlows()).isNotNull();
        assertThat(api.getFlows().size()).isEqualTo(1);

        Flow flow = api.getFlows().get(0);
        Step step = flow.getPre().get(0);

        assertThat(step.getPolicy()).isEqualTo("json-validation");
        assertThat(step.getName()).isEqualTo("JSON Validation");
    }
}
