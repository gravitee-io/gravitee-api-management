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

import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionDescriptor;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsFinder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
class ApiV4DefinitionSecretRefsFinderTest {

    DefinitionSecretRefsFinder<Api> underTest = new ApiV4DefinitionSecretRefsFinder();

    @Test
    void should_can_handle() {
        assertThat(underTest.canHandle(null)).isFalse();
        assertThat(underTest.canHandle(new io.gravitee.definition.model.Api())).isFalse();
        assertThat(underTest.canHandle(new Api())).isTrue();
    }

    @Test
    void should_get_definition() {
        Api api = new Api();
        api.setId("foo");
        assertThat(underTest.toDefinitionDescriptor(api, new DefinitionMetadata(null))).isEqualTo(
            new DefinitionDescriptor(new Definition("api-v4", "foo"), Optional.empty())
        );
        assertThat(underTest.toDefinitionDescriptor(api, new DefinitionMetadata("42"))).isEqualTo(
            new DefinitionDescriptor(new Definition("api-v4", "foo"), Optional.of("42"))
        );
    }

    public static Stream<Arguments> apis() {
        Api empty = new Api();

        Api withResource = new Api();
        withResource.setResources(List.of());

        Api withApiServices = new Api();
        withApiServices.setServices(new ApiServices());

        Api withPlans = new Api();
        withPlans.setPlans(List.of());

        Api withEmptyPlans = new Api();
        withEmptyPlans.setPlans(List.of(new Plan()));

        Api withPlanAndEmptyFlow = new Api();
        Plan planEmptyFlow = new Plan();
        planEmptyFlow.setFlows(List.of());
        withPlanAndEmptyFlow.setPlans(List.of(planEmptyFlow));

        Api withPlanAndFlowNoStep = new Api();
        Plan planFlowNoStep = new Plan();
        planFlowNoStep.setFlows(List.of(new Flow()));
        withPlanAndFlowNoStep.setPlans(List.of(planFlowNoStep));

        Api withEmptyFlow = new Api();
        withEmptyFlow.setFlows(List.of());

        Api withFlowNoStep = new Api();
        withFlowNoStep.setFlows(List.of(new Flow()));

        Api withEmptyListener = new Api();
        withEmptyListener.setListeners(List.of());

        Api withNoEndpointGroup = new Api();
        withNoEndpointGroup.setEndpointGroups(List.of());

        Api withEmptyEndpointGroup = new Api();
        withEmptyEndpointGroup.setEndpointGroups(List.of(new EndpointGroup()));

        Api withEndpointGroupAndEmptyServices = new Api();
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setServices(new EndpointGroupServices());
        withEndpointGroupAndEmptyServices.setEndpointGroups(List.of(endpointGroup));

        Api withEmptyResponseTemplate = new Api();
        withEmptyResponseTemplate.setResponseTemplates(Map.of());

        Api withNoBodyResponseTemplate = new Api();
        ResponseTemplate noBodyRT = new ResponseTemplate();
        noBodyRT.setHeaders(new HashMap<>(Map.of("Foo", "Bar")));
        withNoBodyResponseTemplate.setResponseTemplates(Map.of("foo", Map.of("*/*", noBodyRT)));

        Api withNoHeaderResponseTemplate = new Api();
        ResponseTemplate noHeaderRT = new ResponseTemplate();
        noHeaderRT.setBody("hello");
        withNoHeaderResponseTemplate.setResponseTemplates(Map.of("foo", Map.of("*/*", noHeaderRT)));

        return Stream.of(
            arguments("empty", empty),
            arguments("with resource", withResource),
            arguments("with api services", withApiServices),
            arguments("with plans", withPlans),
            arguments("with empty plans", withEmptyPlans),
            arguments("with plan and empty flow", withPlanAndEmptyFlow),
            arguments("with plan and flow no step", withPlanAndFlowNoStep),
            arguments("with empty flow", withEmptyFlow),
            arguments("with flow no step", withFlowNoStep),
            arguments("with empty listener", withEmptyListener),
            arguments("with empty endpoint group", withEmptyEndpointGroup),
            arguments("with no endpoint group", withNoEndpointGroup),
            arguments("with endpoint group and empty services", withEndpointGroupAndEmptyServices),
            arguments("with no body response template", withNoBodyResponseTemplate),
            arguments("with no header response template", withNoHeaderResponseTemplate)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("apis")
    void should_no_fail_when_parts_of_the_api_is_null(String name, Api api) {
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
        String planRequestFlowConfig = "plan flow request config";
        String planResponseFlowConfig = "plan flow response config";
        String planPublishFlowConfig = "plan flow publish config";
        String planSubscribeFlowConfig = "plan flow subscribe config";
        String definitionRequestFlowConfig = "definition flow request config";
        String definitionResponseFlowConfig = "definition flow response config";
        String definitionPublishFlowConfig = "definition flow publish config";
        String definitionSubscribeFlowConfig = "definition flow subscribe config";
        String planSecurityConfig = "plan security config";
        String endpointGroupHealthCheckServiceConfig = "endpoint group hc service config";
        String endpointGroupDiscoveryServiceConfig = "endpoint group disco service config";
        String endpointGroupSharedConfig = "endpoint group shared config";
        String endpointHealthCheckConfig = "endpoint hc config";
        String endpointConfig = "endpoint config";
        String endpointSharedOverrideConfig = "endpoint shared override config";
        String dynamicPropertyServiceConfig = "dyn property config";
        String responseTemplateBody = "response template body";
        String responseTemplateHeaderValue = "response template header value";
        expectedConfigs.add(entrypointConfig);
        expectedConfigs.add(resourceConfig);
        expectedConfigs.add(planRequestFlowConfig);
        expectedConfigs.add(planResponseFlowConfig);
        expectedConfigs.add(planPublishFlowConfig);
        expectedConfigs.add(planSubscribeFlowConfig);
        expectedConfigs.add(definitionRequestFlowConfig);
        expectedConfigs.add(definitionResponseFlowConfig);
        expectedConfigs.add(definitionPublishFlowConfig);
        expectedConfigs.add(definitionSubscribeFlowConfig);
        expectedConfigs.add(planSecurityConfig);
        expectedConfigs.add(endpointGroupHealthCheckServiceConfig);
        expectedConfigs.add(endpointGroupDiscoveryServiceConfig);
        expectedConfigs.add(endpointGroupSharedConfig);
        expectedConfigs.add(endpointHealthCheckConfig);
        expectedConfigs.add(endpointConfig);
        expectedConfigs.add(endpointSharedOverrideConfig);
        expectedConfigs.add(dynamicPropertyServiceConfig);
        expectedConfigs.add(responseTemplateBody);
        expectedConfigs.add(responseTemplateHeaderValue);

        String entrypointType = "test endpoint type";
        String resourceType = "test resource type";
        String planResponseFlowPolicy = "plan flow response policy";
        String planRequestFlowPolicy = "plan flow request policy";
        String planPublishFlowPolicy = "plan flow publish policy";
        String planSubscribeFlowPolicy = "plan flow subscribe policy";
        String definitionResponseFlowPolicy = "definition flow response policy";
        String definitionRequestFlowPolicy = "definition flow request policy";
        String definitionPublishFlowPolicy = "definition flow publish policy";
        String definitionSubscribeFlowPolicy = "definition flow subscribe policy";
        String planSecurityType = "plan security type";
        String endpointGroupHealthCheckServiceType = "endpoint group hc service type";
        String endpointDiscoveryServiceType = "endpoint group disco service type";
        String endpointGroupType = "endpoint group type";
        String endpointHealthCheckType = "endpoint hc type";
        String endpointType = "endpoint type";
        String dynamicPropertyServiceType = "dyn property type";
        String responseTemplateType = "response template type";
        expectedLocations.add(entrypointType);
        expectedLocations.add(resourceType);
        expectedLocations.add(planRequestFlowPolicy);
        expectedLocations.add(planResponseFlowPolicy);
        expectedLocations.add(planPublishFlowPolicy);
        expectedLocations.add(planSubscribeFlowPolicy);
        expectedLocations.add(definitionResponseFlowPolicy);
        expectedLocations.add(definitionRequestFlowPolicy);
        expectedLocations.add(definitionPublishFlowPolicy);
        expectedLocations.add(definitionSubscribeFlowPolicy);
        expectedLocations.add(planSecurityType);
        expectedLocations.add(endpointGroupHealthCheckServiceType);
        expectedLocations.add(endpointDiscoveryServiceType);
        expectedLocations.add(endpointGroupType);
        expectedLocations.add(endpointHealthCheckType);
        expectedLocations.add(endpointType);
        expectedLocations.add(dynamicPropertyServiceType);
        expectedLocations.add(responseTemplateType);

        // pre-test check (there are 2x two configs for the same location)
        assertThat(expectedConfigs).hasSize(expectedLocations.size() + 2);

        Api api = new Api();
        Listener httpListener = new HttpListener();
        Entrypoint entryPoint = new Entrypoint();
        entryPoint.setType(entrypointType);
        entryPoint.setConfiguration(entrypointConfig);
        httpListener.setEntrypoints(List.of(entryPoint));
        api.setListeners(List.of(httpListener));
        Resource resource = new Resource();
        resource.setType(resourceType);
        resource.setConfiguration(resourceConfig);
        api.setResources(List.of(resource));
        Plan plan = new Plan();
        PlanSecurity security = new PlanSecurity();
        security.setConfiguration(planSecurityConfig);
        security.setType(planSecurityType);
        plan.setSecurity(security);
        Flow planFlow = new Flow();
        planFlow.setRequest(List.of(newStep(planRequestFlowConfig, planRequestFlowPolicy)));
        planFlow.setResponse(List.of(newStep(planResponseFlowConfig, planResponseFlowPolicy)));
        planFlow.setPublish(List.of(newStep(planPublishFlowConfig, planPublishFlowPolicy)));
        plan.setFlows(List.of(planFlow));
        api.setPlans(List.of(plan));
        planFlow.setSubscribe(List.of(newStep(planSubscribeFlowConfig, planSubscribeFlowPolicy)));
        Flow flow = new Flow();
        flow.setRequest(List.of(newStep(definitionRequestFlowConfig, definitionRequestFlowPolicy)));
        flow.setResponse(List.of(newStep(definitionResponseFlowConfig, definitionResponseFlowPolicy)));
        flow.setPublish(List.of(newStep(definitionPublishFlowConfig, definitionPublishFlowPolicy)));
        flow.setSubscribe(List.of(newStep(definitionSubscribeFlowConfig, definitionSubscribeFlowPolicy)));
        api.setFlows(List.of(flow));
        EndpointGroup endpointGroup = new EndpointGroup();
        EndpointGroupServices endpointGroupServices = new EndpointGroupServices();
        Service endpointHealthCheckService = new Service();
        endpointHealthCheckService.setConfiguration(endpointGroupHealthCheckServiceConfig);
        endpointHealthCheckService.setType(endpointGroupHealthCheckServiceType);
        endpointGroupServices.setHealthCheck(endpointHealthCheckService);
        Service discoveryService = new Service();
        discoveryService.setConfiguration(endpointGroupDiscoveryServiceConfig);
        discoveryService.setType(endpointDiscoveryServiceType);
        endpointGroupServices.setDiscovery(discoveryService);
        endpointGroup.setServices(endpointGroupServices);
        endpointGroup.setSharedConfiguration(endpointGroupSharedConfig);
        endpointGroup.setType(endpointGroupType);
        Endpoint endpoint = new Endpoint();
        EndpointServices endpointServices = new EndpointServices();
        Service endpointHealthCheck = new Service();
        endpointHealthCheck.setType(endpointHealthCheckType);
        endpointHealthCheck.setConfiguration(endpointHealthCheckConfig);
        endpointServices.setHealthCheck(endpointHealthCheck);
        endpoint.setServices(endpointServices);
        endpoint.setType(endpointType);
        endpoint.setConfiguration(endpointConfig);
        endpoint.setSharedConfigurationOverride(endpointSharedOverrideConfig);
        endpointGroup.setEndpoints(List.of(endpoint));
        api.setEndpointGroups(List.of(endpointGroup));
        ApiServices apiServices = new ApiServices();
        Service dynamicPropertyService = new Service();
        dynamicPropertyService.setType(dynamicPropertyServiceType);
        dynamicPropertyService.setConfiguration(dynamicPropertyServiceConfig);
        apiServices.setDynamicProperty(dynamicPropertyService);
        api.setServices(apiServices);
        ResponseTemplate responseTemplate = new ResponseTemplate();
        responseTemplate.setBody(responseTemplateBody);
        responseTemplate.setHeaders(new HashMap<>(Map.of("Test", responseTemplateHeaderValue)));
        api.setResponseTemplates(Map.of(responseTemplateType, Map.of("*/*", responseTemplate)));

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
        assertThat(api.getPlans().get(0).getFlows().get(0).getRequest().get(0).getConfiguration()).isEqualTo(
            processed(planRequestFlowConfig)
        );
        assertThat(api.getPlans().get(0).getFlows().get(0).getResponse().get(0).getConfiguration()).isEqualTo(
            processed(planResponseFlowConfig)
        );
        assertThat(api.getPlans().get(0).getFlows().get(0).getPublish().get(0).getConfiguration()).isEqualTo(
            processed(planPublishFlowConfig)
        );
        assertThat(api.getPlans().get(0).getFlows().get(0).getSubscribe().get(0).getConfiguration()).isEqualTo(
            processed(planSubscribeFlowConfig)
        );
        assertThat(api.getFlows().get(0).getRequest().get(0).getConfiguration()).isEqualTo(processed(definitionRequestFlowConfig));
        assertThat(api.getFlows().get(0).getResponse().get(0).getConfiguration()).isEqualTo(processed(definitionResponseFlowConfig));
        assertThat(api.getFlows().get(0).getPublish().get(0).getConfiguration()).isEqualTo(processed(definitionPublishFlowConfig));
        assertThat(api.getFlows().get(0).getSubscribe().get(0).getConfiguration()).isEqualTo(processed(definitionSubscribeFlowConfig));
        assertThat(api.getEndpointGroups().get(0).getServices().getHealthCheck().getConfiguration()).isEqualTo(
            processed(endpointGroupHealthCheckServiceConfig)
        );
        assertThat(api.getEndpointGroups().get(0).getServices().getDiscovery().getConfiguration()).isEqualTo(
            processed(endpointGroupDiscoveryServiceConfig)
        );
        assertThat(api.getEndpointGroups().get(0).getSharedConfiguration()).isEqualTo(processed(endpointGroupSharedConfig));
        assertThat(api.getEndpointGroups().get(0).getEndpoints().get(0).getConfiguration()).isEqualTo(processed(endpointConfig));
        assertThat(api.getEndpointGroups().get(0).getEndpoints().get(0).getServices().getHealthCheck().getConfiguration()).isEqualTo(
            processed(endpointHealthCheckConfig)
        );
        ResponseTemplate processedTemplate = api.getResponseTemplates().get(responseTemplateType).get("*/*");
        assertThat(processedTemplate.getBody()).isEqualTo(processed(responseTemplateBody));
        assertThat(processedTemplate.getHeaders().get("Test")).hasToString(processed(responseTemplateHeaderValue));
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
