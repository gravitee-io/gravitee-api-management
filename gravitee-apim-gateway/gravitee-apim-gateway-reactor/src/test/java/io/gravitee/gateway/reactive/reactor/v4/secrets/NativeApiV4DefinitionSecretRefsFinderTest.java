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
package io.gravitee.gateway.reactive.reactor.v4.secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.nativeapi.*;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionDescriptor;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsFinder;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeApiV4DefinitionSecretRefsFinderTest {

    DefinitionSecretRefsFinder<NativeApi> underTest = new NativeApiV4DefinitionSecretRefsFinder();

    @Test
    void should_can_handle() {
        assertThat(underTest.canHandle(null)).isFalse();
        assertThat(underTest.canHandle(new io.gravitee.definition.model.v4.Api())).isFalse();
        assertThat(underTest.canHandle(new NativeApi())).isTrue();
    }

    @Test
    void should_get_definition() {
        NativeApi api = new NativeApi();
        api.setId("foo");
        assertThat(underTest.toDefinitionDescriptor(api, new DefinitionMetadata(null))).isEqualTo(
            new DefinitionDescriptor(new Definition("native-api-v4", "foo"), Optional.empty())
        );
        assertThat(underTest.toDefinitionDescriptor(api, new DefinitionMetadata("42"))).isEqualTo(
            new DefinitionDescriptor(new Definition("native-api-v4", "foo"), Optional.of("42"))
        );
    }

    public static Stream<Arguments> apis() {
        NativeApi empty = new NativeApi();

        NativeApi withResource = new NativeApi();
        withResource.setResources(List.of());

        NativeApi withPlans = new NativeApi();
        withPlans.setPlans(List.of());

        NativeApi withEmptyPlans = new NativeApi();
        withEmptyPlans.setPlans(List.of(new NativePlan()));

        NativeApi withPlanAndEmptyFlow = new NativeApi();
        NativePlan planEmptyFlow = new NativePlan();
        planEmptyFlow.setFlows(List.of());
        withPlanAndEmptyFlow.setPlans(List.of(planEmptyFlow));

        NativeApi withPlanAndFlowNoStep = new NativeApi();
        NativePlan planFlowNoStep = new NativePlan();
        planFlowNoStep.setFlows(List.of(new NativeFlow()));
        withPlanAndFlowNoStep.setPlans(List.of(planFlowNoStep));

        NativeApi withEmptyFlow = new NativeApi();
        withEmptyFlow.setFlows(List.of());

        NativeApi withFlowNoStep = new NativeApi();
        withFlowNoStep.setFlows(List.of(new NativeFlow()));

        NativeApi withEmptyListener = new NativeApi();
        withEmptyListener.setListeners(List.of());

        NativeApi withNoEndpointGroup = new NativeApi();
        withNoEndpointGroup.setEndpointGroups(List.of());

        NativeApi withEmptyEndpointGroup = new NativeApi();
        withEmptyEndpointGroup.setEndpointGroups(List.of(new NativeEndpointGroup()));

        return Stream.of(
            arguments("empty", empty),
            arguments("with resource", withResource),
            arguments("with plans", withPlans),
            arguments("with empty plans", withEmptyPlans),
            arguments("with plan and empty flow", withPlanAndEmptyFlow),
            arguments("with plan and flow no step", withPlanAndFlowNoStep),
            arguments("with empty flow", withEmptyFlow),
            arguments("with flow no step", withFlowNoStep),
            arguments("with empty listener", withEmptyListener),
            arguments("with empty endpoint group", withEmptyEndpointGroup),
            arguments("with no endpoint group", withNoEndpointGroup)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("apis")
    void should_no_fail_when_parts_of_the_api_is_null(String name, NativeApi api) {
        assertThatCode(() ->
            underTest.findSecretRefs(api, (config, location, setter) -> setter.accept(processed(config)))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_find_secrets() {
        Set<String> expectedConfigs = new FailOnDuplicateSet<>();
        Set<String> expectedLocations = new FailOnDuplicateSet<>();

        String entrypointConfig = "entrypoint config";
        String resourceConfig = "resource config";
        String planPublishFlowConfig = "plan flow publish config";
        String planSubscribeFlowConfig = "plan flow subscribe config";
        String planEntrypointConnectFlowConfig = "plan flow entrypoint connect config";
        String planInteractFlowConfig = "plan flow interact config";
        String definitionPublishFlowConfig = "definition flow publish config";
        String definitionSubscribeFlowConfig = "definition flow subscribe config";
        String definitionEntrypointConnectFlowConfig = "definition flow entrypoint connect config";
        String definitionInteractFlowConfig = "definition flow interact config";
        String planSecurityConfig = "plan security config";
        String endpointGroupSharedConfig = "endpoint group shared config";
        String endpointConfig = "endpoint config";
        String endpointSharedOverrideConfig = "endpoint shared override config";
        String dynamicPropertyServiceConfig = "dyn property config";
        expectedConfigs.add(entrypointConfig);
        expectedConfigs.add(resourceConfig);
        expectedConfigs.add(planPublishFlowConfig);
        expectedConfigs.add(planSubscribeFlowConfig);
        expectedConfigs.add(planEntrypointConnectFlowConfig);
        expectedConfigs.add(planInteractFlowConfig);
        expectedConfigs.add(definitionPublishFlowConfig);
        expectedConfigs.add(definitionSubscribeFlowConfig);
        expectedConfigs.add(definitionEntrypointConnectFlowConfig);
        expectedConfigs.add(definitionInteractFlowConfig);
        expectedConfigs.add(planSecurityConfig);
        expectedConfigs.add(endpointGroupSharedConfig);
        expectedConfigs.add(endpointConfig);
        expectedConfigs.add(endpointSharedOverrideConfig);
        expectedConfigs.add(dynamicPropertyServiceConfig);

        String entrypointType = "test endpoint type";
        String resourceType = "test resource type";
        String planPublishFlowPolicy = "plan flow publish policy";
        String planSubscribeFlowPolicy = "plan flow subscribe policy";
        String planEntrypointConnectFlowPolicy = "plan flow entrypoint connect policy";
        String planInteractFlowPolicy = "plan flow interact policy";
        String definitionPublishFlowPolicy = "definition flow publish policy";
        String definitionSubscribeFlowPolicy = "definition flow subscribe policy";
        String definitionEntrypointConnectFlowPolicy = "definition flow entrypoint connect policy";
        String definitionInteractFlowPolicy = "definition flow interact policy";
        String planSecurityType = "plan security type";
        String endpointGroupType = "endpoint group type";
        String endpointType = "endpoint type";
        String dynamicPropertyServiceType = "dyn property type";
        expectedLocations.add(entrypointType);
        expectedLocations.add(resourceType);
        expectedLocations.add(planPublishFlowPolicy);
        expectedLocations.add(planSubscribeFlowPolicy);
        expectedLocations.add(planEntrypointConnectFlowPolicy);
        expectedLocations.add(planInteractFlowPolicy);
        expectedLocations.add(definitionPublishFlowPolicy);
        expectedLocations.add(definitionSubscribeFlowPolicy);
        expectedLocations.add(definitionEntrypointConnectFlowPolicy);
        expectedLocations.add(definitionInteractFlowPolicy);
        expectedLocations.add(planSecurityType);
        expectedLocations.add(endpointGroupType);
        expectedLocations.add(endpointType);
        expectedLocations.add(dynamicPropertyServiceType);

        // pre-test check (+1 because endpoint type is used twice)
        assertThat(expectedConfigs).hasSize(expectedLocations.size() + 1);

        NativeApi api = new NativeApi();
        NativeListener kafkaListener = new KafkaListener();
        NativeEntrypoint entryPoint = new NativeEntrypoint();
        entryPoint.setType(entrypointType);
        entryPoint.setConfiguration(entrypointConfig);
        kafkaListener.setEntrypoints(List.of(entryPoint));
        api.setListeners(List.of(kafkaListener));
        Resource resource = new Resource();
        resource.setType(resourceType);
        resource.setConfiguration(resourceConfig);
        api.setResources(List.of(resource));
        NativePlan plan = new NativePlan();
        PlanSecurity security = new PlanSecurity();
        security.setConfiguration(planSecurityConfig);
        security.setType(planSecurityType);
        plan.setSecurity(security);
        NativeFlow planFlow = new NativeFlow();
        planFlow.setPublish(List.of(newStep(planPublishFlowConfig, planPublishFlowPolicy)));
        planFlow.setSubscribe(List.of(newStep(planSubscribeFlowConfig, planSubscribeFlowPolicy)));
        planFlow.setEntrypointConnect(List.of(newStep(planEntrypointConnectFlowConfig, planEntrypointConnectFlowPolicy)));
        planFlow.setInteract(List.of(newStep(planInteractFlowConfig, planInteractFlowPolicy)));
        plan.setFlows(List.of(planFlow));
        api.setPlans(List.of(plan));
        NativeFlow flow = new NativeFlow();
        flow.setEntrypointConnect(List.of(newStep(definitionEntrypointConnectFlowConfig, definitionEntrypointConnectFlowPolicy)));
        flow.setInteract(List.of(newStep(definitionInteractFlowConfig, definitionInteractFlowPolicy)));
        flow.setPublish(List.of(newStep(definitionPublishFlowConfig, definitionPublishFlowPolicy)));
        flow.setSubscribe(List.of(newStep(definitionSubscribeFlowConfig, definitionSubscribeFlowPolicy)));
        api.setFlows(List.of(flow));
        NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
        endpointGroup.setSharedConfiguration(endpointGroupSharedConfig);
        endpointGroup.setType(endpointGroupType);
        NativeEndpoint endpoint = new NativeEndpoint();
        endpoint.setType(endpointType);
        endpoint.setConfiguration(endpointConfig);
        endpoint.setSharedConfigurationOverride(endpointSharedOverrideConfig);
        endpointGroup.setEndpoints(List.of(endpoint));
        api.setEndpointGroups(List.of(endpointGroup));
        NativeApiServices apiServices = new NativeApiServices();
        Service dynamicPropertyService = new Service();
        dynamicPropertyService.setType(dynamicPropertyServiceType);
        dynamicPropertyService.setConfiguration(dynamicPropertyServiceConfig);
        apiServices.setDynamicProperty(dynamicPropertyService);
        api.setServices(apiServices);

        //data gathering
        Set<String> actualConfigs = new HashSet<>();
        Set<String> actualLocations = new HashSet<>();

        // execute test
        underTest.findSecretRefs(api, (config, location, setter) -> {
            actualConfigs.add(config);
            actualLocations.add(location.id());
            // simulate plugin has processed the config
            String processed = processed(config);
            setter.accept(processed);
        });

        // assertions
        assertThat(actualConfigs).hasSameElementsAs(expectedConfigs);
        assertThat(actualLocations).hasSameElementsAs(expectedLocations);

        assertThat(api.getListeners().get(0).getEntrypoints().get(0).getConfiguration()).isEqualTo(processed(entrypointConfig));
        assertThat(api.getResources().get(0).getConfiguration()).isEqualTo(processed(resourceConfig));
        assertThat(api.getPlans().get(0).getSecurity().getConfiguration()).isEqualTo(processed(planSecurityConfig));
        assertThat(api.getPlans().get(0).getFlows().get(0).getPublish().get(0).getConfiguration()).isEqualTo(
            processed(planPublishFlowConfig)
        );
        assertThat(api.getPlans().get(0).getFlows().get(0).getSubscribe().get(0).getConfiguration()).isEqualTo(
            processed(planSubscribeFlowConfig)
        );
        assertThat(api.getPlans().get(0).getFlows().get(0).getEntrypointConnect().get(0).getConfiguration()).isEqualTo(
            processed(planEntrypointConnectFlowConfig)
        );
        assertThat(api.getPlans().get(0).getFlows().get(0).getInteract().get(0).getConfiguration()).isEqualTo(
            processed(planInteractFlowConfig)
        );
        assertThat(api.getFlows().get(0).getPublish().get(0).getConfiguration()).isEqualTo(processed(definitionPublishFlowConfig));
        assertThat(api.getFlows().get(0).getSubscribe().get(0).getConfiguration()).isEqualTo(processed(definitionSubscribeFlowConfig));
        assertThat(api.getFlows().get(0).getEntrypointConnect().get(0).getConfiguration()).isEqualTo(
            processed(definitionEntrypointConnectFlowConfig)
        );
        assertThat(api.getFlows().get(0).getInteract().get(0).getConfiguration()).isEqualTo(processed(definitionInteractFlowConfig));
        assertThat(api.getEndpointGroups().get(0).getSharedConfiguration()).isEqualTo(processed(endpointGroupSharedConfig));
        assertThat(api.getEndpointGroups().get(0).getEndpoints().get(0).getConfiguration()).isEqualTo(processed(endpointConfig));
    }

    private static Step newStep(String configuration, String policy) {
        Step requestStep = new Step();
        requestStep.setConfiguration(configuration);
        requestStep.setPolicy(policy);
        return requestStep;
    }

    String processed(String original) {
        return original.concat(" - updated!");
    }

    private static class FailOnDuplicateSet<T> extends LinkedHashSet<T> {

        @Override
        public boolean add(T o) {
            if (!super.add(o)) {
                throw new IllegalArgumentException("[" + o + "] exists in set, test cannot work");
            }
            return false;
        }
    }
}
