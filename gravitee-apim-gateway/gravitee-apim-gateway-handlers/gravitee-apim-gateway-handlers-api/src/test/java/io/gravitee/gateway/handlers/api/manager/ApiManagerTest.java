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
package io.gravitee.gateway.handlers.api.manager;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graviteesource.services.runtimesecrets.RuntimeSecretsService;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.impl.ApiManagerImpl;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.license.ForbiddenFeatureException;
import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.LicenseManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiManagerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EventManager eventManager;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private DataEncryptor dataEncryptor;

    @Mock
    private ClusterManager clusterManager;

    @Mock
    private LicenseManager licenseManager;

    @Mock
    RuntimeSecretsService runtimeSecretsService;

    private ApiManagerImpl apiManager;

    @Before
    public void setUp() throws Exception {
        apiManager = spy(new ApiManagerImpl(eventManager, gatewayConfiguration, licenseManager, dataEncryptor, runtimeSecretsService));
        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.empty());
        when(gatewayConfiguration.hasMatchingTags(any())).thenCallRealMethod();
    }

    @Test
    public void shouldNotDeployDisableApi() {
        final Api api = buildTestApi();
        api.setEnabled(false);

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void shouldNotDeployApiWithoutPlan() {
        final Api api = buildTestApi();

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
        assertEquals(0, apiManager.apis().size());
    }

    @Test
    public void shouldDeployApiWithPlan() {
        final Api api = buildTestApi();
        final Plan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
        assertEquals(1, apiManager.apis().size());
    }

    @Test
    public void shouldNotDeployApiWithTagOnGatewayTagInInclusionAndExclusion() {
        final Api api = buildTestApi();
        final Plan mockedPlan = mock(Plan.class);

        api.getDefinition().setPlans(singletonList(mockedPlan));
        api.getDefinition().setTags(new HashSet<>(Arrays.asList("test")));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList("test,!test")));

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void shouldDeployApiWithTagOnGatewayWithoutTag() {
        final Api api = buildTestApi();
        final Plan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));
        api.getDefinition().setTags(new HashSet<>(singletonList("test")));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void shouldNotDeployApiWithTagOnGatewayTagExclusion() {
        final Api api = buildTestApi();
        final Plan mockedPlan = mock(Plan.class);

        api.getDefinition().setPlans(singletonList(mockedPlan));
        api.getDefinition().setTags(new HashSet<>(Arrays.asList("product", "international")));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList("product", "!international")));

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void shouldNotDeployApiWithTagOnGatewayWithoutTag() {
        final Api api = buildTestApi();
        final Plan mockedPlan = mock(Plan.class);

        api.getDefinition().setPlans(singletonList(mockedPlan));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("product")));

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    private void shouldDeployApiWithTags(final String tags, final String[] apiTags) {
        final Api api = buildTestApi();
        final Plan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));
        api.getDefinition().setTags(new HashSet<>(Arrays.asList(apiTags)));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList(tags.split(","))));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void shouldDeployApiWithPlanMatchingTag() {
        final Api api = buildTestApi();
        api.getDefinition().setTags(new HashSet<>(singletonList("test")));

        final Plan mockedPlan = buildMockPlan();
        when(mockedPlan.getTags()).thenReturn(new HashSet<>(singletonList("test")));
        api.getDefinition().setPlans(singletonList(mockedPlan));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void shouldNotDeployApiWithoutPlanMatchingTag() {
        final Api api = buildTestApi();
        api.getDefinition().setTags(new HashSet<>(singletonList("test")));

        final Plan mockedPlan = mock(Plan.class);
        when(mockedPlan.getStatus()).thenReturn("published");
        when(mockedPlan.getTags()).thenReturn(new HashSet<>(singletonList("test2")));
        api.getDefinition().setPlans(singletonList(mockedPlan));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void test_deployApiWithTag() throws Exception {
        shouldDeployApiWithTags("test,toto", new String[] { "test" });
    }

    @Test
    public void test_deployApiWithTagExcluded() throws Exception {
        shouldDeployApiWithTags("!test", new String[] { "toto" });
    }

    @Test
    public void test_deployApiWithUpperCasedTag() throws Exception {
        shouldDeployApiWithTags("test,toto", new String[] { "Test" });
    }

    @Test
    public void test_deployApiWithAccentTag() throws Exception {
        shouldDeployApiWithTags("test,toto", new String[] { "tést" });
    }

    @Test
    public void test_deployApiWithUpperCasedAndAccentTag() throws Exception {
        shouldDeployApiWithTags("test", new String[] { "Tést" });
    }

    @Test
    public void test_deployApiWithTagExclusion() throws Exception {
        shouldDeployApiWithTags("test,!toto", new String[] { "test" });
    }

    @Test
    public void test_deployApiWithSpaceAfterComma() throws Exception {
        shouldDeployApiWithTags("test, !toto", new String[] { "test" });
    }

    @Test
    public void test_deployApiWithSpaceBeforeComma() throws Exception {
        shouldDeployApiWithTags("test ,!toto", new String[] { "test" });
    }

    @Test
    public void test_deployApiWithSpaceBeforeTag() throws Exception {
        shouldDeployApiWithTags(" test,!toto", new String[] { "test" });
    }

    @Test
    public void shouldUpdateApi() {
        final Api api = buildTestApi();
        final Plan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        final Api api2 = buildTestApi();
        Instant deployDateInst = api.getDeployedAt().toInstant().plus(Duration.ofHours(1));
        api2.setDeployedAt(Date.from(deployDateInst));
        api2.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api2);

        verify(eventManager).publishEvent(ReactorEvent.UPDATE, api2);
    }

    @Test
    public void shouldNotUpdateApi() {
        final Api api = buildTestApi();
        final Plan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        final Api api2 = buildTestApi();
        Instant deployDateInst = api.getDeployedAt().toInstant().minus(Duration.ofHours(1));
        api2.setDeployedAt(Date.from(deployDateInst));

        apiManager.register(api2);

        verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
    }

    @Test
    public void shouldUndeployApi_noMoreMatchingTag() {
        final Api api = buildTestApi();
        final Plan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));
        api.getDefinition().setTags(new HashSet<>(singletonList("test")));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        final Api api2 = buildTestApi();
        api2.setDeployedAt(new Date());
        api2.getDefinition().setTags(new HashSet<>(singletonList("other-tag")));

        apiManager.register(api2);

        verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
    }

    private Plan buildMockPlan() {
        Plan plan = mock(Plan.class);
        when(plan.getStatus()).thenReturn("published");

        return plan;
    }

    @Test
    public void shouldUndeployApi() {
        final Api api = buildTestApi();
        final Plan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        apiManager.unregister(api.getId());

        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
    }

    @Test
    public void shouldNotUndeployUnknownApi() {
        final Api api = buildTestApi();
        final Plan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        apiManager.unregister("unknown-api");

        verify(eventManager, never()).publishEvent(ReactorEvent.UNDEPLOY, api);
    }

    @Test
    public void shouldUndeployApi_noMorePlan() {
        final Api api = buildTestApi();
        final Plan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        final Api api2 = buildTestApi();
        api2.setDeployedAt(new Date(api.getDeployedAt().getTime() + 100));
        api2.getDefinition().setPlans(Collections.emptyList());

        apiManager.register(api2);

        verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
    }

    @Test
    public void shouldDecryptApiPropertiesOnDeployment() throws Exception {
        final Api api = buildTestApi();

        Properties properties = new Properties();
        properties.setProperties(
            List.of(
                new Property("key1", "plain value 1", false),
                new Property("key2", "value2Base64encrypted", true),
                new Property("key3", "value3Base64encrypted", true)
            )
        );
        api.getDefinition().setProperties(properties);

        when(dataEncryptor.decrypt("value2Base64encrypted")).thenReturn("plain value 2");
        when(dataEncryptor.decrypt("value3Base64encrypted")).thenReturn("plain value 3");

        apiManager.register(api);

        verify(dataEncryptor, times(2)).decrypt(any());
        assertEquals(
            Map.of("key1", "plain value 1", "key2", "plain value 2", "key3", "plain value 3"),
            api.getDefinition().getProperties().getValues()
        );
    }

    @Test
    public void shouldRequireDeploymentWithNoApi() {
        final Api api = buildTestApi();
        final Plan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
        assertEquals(ActionOnApi.DEPLOY, actionOnApi);
    }

    @Test
    public void shouldRequireDeploymentWithUpdatedApi() {
        final Api api = buildTestApi();
        final Plan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api);
        ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
        assertEquals(ActionOnApi.NONE, actionOnApi);

        final Api api2 = buildTestApi();
        Instant deployDateInst = api.getDeployedAt().toInstant().plus(Duration.ofHours(1));
        api2.setDeployedAt(Date.from(deployDateInst));
        api2.getDefinition().setPlans(singletonList(mockedPlan));

        ActionOnApi actionOnApi2 = apiManager.requiredActionFor(api2);
        assertEquals(ActionOnApi.DEPLOY, actionOnApi2);
    }

    @Test
    public void shouldNotRequireDeploymentWithSameApi() {
        final Api api = buildTestApi();
        final Plan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api);
        ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
        assertEquals(ActionOnApi.NONE, actionOnApi);
    }

    @Test
    public void shouldRequireDeploymentWithMatchingTag() {
        final Api api = buildTestApi();
        api.getDefinition().setTags(new HashSet<>(singletonList("test")));
        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

        ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
        assertEquals(ActionOnApi.DEPLOY, actionOnApi);
    }

    @Test
    public void shouldNotRequireDeploymentWithoutMatchingTagAndApiNotAlreadyDeployed() {
        final Api api = buildTestApi();
        api.getDefinition().setTags(new HashSet<>(singletonList("test2")));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

        ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
        assertEquals(ActionOnApi.NONE, actionOnApi);
    }

    @Test
    public void shouldRequireUndeploymentWithoutMatchingTagAndApiAlreadyDeployed() {
        final Api api = buildTestApi();
        final Plan mockedPlan = buildMockPlan();
        api.getDefinition().setPlans(singletonList(mockedPlan));
        apiManager.register(api);

        api.getDefinition().setTags(new HashSet<>(singletonList("test2")));
        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

        ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
        assertEquals(ActionOnApi.UNDEPLOY, actionOnApi);
    }

    @Test
    @SneakyThrows
    public void shouldNotDeployIfLicenseIsInvalid() {
        var api = buildTestApi();
        final String orgId = api.getOrganizationId();
        doThrow(new InvalidLicenseException("Invalid license for test"))
            .when(licenseManager)
            .validatePluginFeatures(eq(orgId), anyCollection());
        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    @SneakyThrows
    public void shouldNotDeployIfApiUsesFeatureNotEntitledByLicense() {
        var api = buildTestApi();
        final String orgId = api.getOrganizationId();
        doThrow(new ForbiddenFeatureException(List.of(new LicenseManager.ForbiddenFeature("Not entitled feature", "some plugin"))))
            .when(licenseManager)
            .validatePluginFeatures(eq(orgId), anyCollection());
        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    private Api mockApi(final io.gravitee.repository.management.model.Api api) throws Exception {
        return mockApi(api, new String[] {});
    }

    private Api mockApi(final io.gravitee.repository.management.model.Api api, final String[] tags) throws Exception {
        final io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        final Api api2 = new Api(definition);
        definition.setId(api.getId());
        definition.setName(api.getName());
        definition.setTags(new HashSet<>(Arrays.asList(tags)));
        when(objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.Api.class)).thenReturn(definition);
        return api2;
    }

    private Api buildTestApi() {
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(singletonList(mock(VirtualHost.class)));
        return new ApiBuilder().id("api-test").name("api-name-test").proxy(proxy).deployedAt(new Date()).build();
    }

    class ApiBuilder {

        private final io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        private final Api api = new Api(definition);

        public ApiBuilder id(String id) {
            this.definition.setId(id);
            return this;
        }

        public ApiBuilder name(String name) {
            this.definition.setName(name);
            return this;
        }

        public ApiBuilder proxy(Proxy proxy) {
            this.definition.setProxy(proxy);
            return this;
        }

        public ApiBuilder deployedAt(Date updatedAt) {
            this.api.setDeployedAt(updatedAt);
            return this;
        }

        public Api build() {
            api.setEnabled(true);

            return this.api;
        }
    }
}
