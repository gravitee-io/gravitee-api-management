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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyCollection;
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
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.manager.impl.ApiManagerImpl;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactor.ReactorEvent;
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
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiManagerV4Test {

    private ApiManagerImpl apiManager;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EventManager eventManager;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private DataEncryptor dataEncryptor;

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private RuntimeSecretsService runtimeSecretsService;

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
        final Plan mockedPlan = buildMockPlan();

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
        when(mockedPlan.getStatus()).thenReturn(PlanStatus.PUBLISHED);
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
        when(plan.getStatus()).thenReturn(PlanStatus.PUBLISHED);

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

        api
            .getDefinition()
            .setProperties(
                List.of(
                    new Property("key1", "plain value 1", false),
                    new Property("key2", "value2Base64encrypted", true),
                    new Property("key3", "value3Base64encrypted", true)
                )
            );

        when(dataEncryptor.decrypt("value2Base64encrypted")).thenReturn("plain value 2");
        when(dataEncryptor.decrypt("value3Base64encrypted")).thenReturn("plain value 3");

        apiManager.register(api);

        verify(dataEncryptor, times(2)).decrypt(any());
        assertEquals(
            Map.of("key1", "plain value 1", "key2", "plain value 2", "key3", "plain value 3"),
            api.getDefinition().getProperties().stream().collect(Collectors.toMap(Property::getKey, Property::getValue))
        );
    }

    @Test
    @SneakyThrows
    public void shouldNotDeployIfLicenseIsInvalid() {
        final Api api = buildTestApi();
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
        final Api api = buildTestApi();
        final String orgId = api.getOrganizationId();
        doThrow(new ForbiddenFeatureException(List.of(new LicenseManager.ForbiddenFeature("Not entitled feature", "some plugin"))))
            .when(licenseManager)
            .validatePluginFeatures(eq(orgId), anyCollection());
        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    private Api buildTestApi() {
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(mock(Path.class)));
        return new ApiBuilder()
            .id("api-test")
            .name("api-name-test")
            .listeners(List.of(httpListener))
            .deployedAt(new Date())
            .organizationId("org-id")
            .build();
    }

    class ApiBuilder {

        private final io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        private final Api api = new Api(definition);

        public ApiBuilder id(String id) {
            this.definition.setId(id);
            return this;
        }

        public ApiBuilder name(String name) {
            this.definition.setName(name);
            return this;
        }

        public ApiBuilder listeners(List<Listener> listeners) {
            this.definition.setListeners(listeners);
            return this;
        }

        public ApiBuilder deployedAt(Date updatedAt) {
            this.api.setDeployedAt(updatedAt);
            return this;
        }

        public ApiBuilder organizationId(String organizationId) {
            this.api.setOrganizationId(organizationId);
            return this;
        }

        public Api build() {
            api.setEnabled(true);

            return this.api;
        }
    }
}
