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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.v4.nativeapi.NativeListener;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.manager.impl.ApiManagerImpl;
import io.gravitee.gateway.reactive.handlers.api.v4.NativeApi;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiManagerNativeTest {

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

    @BeforeEach
    public void setUp() throws Exception {
        apiManager = spy(new ApiManagerImpl(eventManager, gatewayConfiguration, licenseManager, dataEncryptor));
        lenient().when(gatewayConfiguration.shardingTags()).thenReturn(Optional.empty());
        lenient().when(gatewayConfiguration.hasMatchingTags(any())).thenCallRealMethod();
    }

    @Test
    public void should_not_deploy_disabled_api() {
        final NativeApi api = buildTestApi();
        api.setEnabled(false);

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void should_not_deploy_api_without_plan() {
        final NativeApi api = buildTestApi();

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
        assertEquals(0, apiManager.apis().size());
    }

    @Test
    public void should_deploy_api_with_plan() {
        final NativeApi api = buildTestApi();
        final NativePlan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
        assertEquals(1, apiManager.apis().size());
    }

    @Test
    public void should_not_deploy_api_with_tag_on_gateway_tag_in_inclusion_and_exclusion() {
        final NativeApi api = buildTestApi();
        final NativePlan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));
        api.getDefinition().setTags(new HashSet<>(Arrays.asList("test")));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList("test,!test")));

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void should_deploy_api_with_tag_on_gateway_without_tag() {
        final NativeApi api = buildTestApi();
        final NativePlan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));
        api.getDefinition().setTags(new HashSet<>(singletonList("test")));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void should_not_deploy_api_with_tag_on_gateway_tag_exclusion() {
        final NativeApi api = buildTestApi();
        final NativePlan mockedPlan = mock(NativePlan.class);

        api.getDefinition().setPlans(singletonList(mockedPlan));
        api.getDefinition().setTags(new HashSet<>(Arrays.asList("product", "international")));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList("product", "!international")));

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void should_not_deploy_api_with_tag_on_gateway_without_tag() {
        final NativeApi api = buildTestApi();
        final NativePlan mockedPlan = mock(NativePlan.class);

        api.getDefinition().setPlans(singletonList(mockedPlan));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("product")));

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    private void shouldDeployApiWithTags(final String tags, final String[] apiTags) {
        final NativeApi api = buildTestApi();
        final NativePlan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));
        api.getDefinition().setTags(new HashSet<>(Arrays.asList(apiTags)));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList(tags.split(","))));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void test_deploy_api_with_tag() throws Exception {
        shouldDeployApiWithTags("test,toto", new String[] { "test" });
    }

    @Test
    public void test_deploy_api_with_tag_excluded() throws Exception {
        shouldDeployApiWithTags("!test", new String[] { "toto" });
    }

    @Test
    public void test_deploy_api_with_upper_cased_tag() throws Exception {
        shouldDeployApiWithTags("test,toto", new String[] { "Test" });
    }

    @Test
    public void test_deploy_api_with_accent_tag() throws Exception {
        shouldDeployApiWithTags("test,toto", new String[] { "tést" });
    }

    @Test
    public void test_deploy_api_with_upper_cased_and_accent_tag() throws Exception {
        shouldDeployApiWithTags("test", new String[] { "Tést" });
    }

    @Test
    public void test_deploy_api_with_tag_exclusion() throws Exception {
        shouldDeployApiWithTags("test,!toto", new String[] { "test" });
    }

    @Test
    public void test_deploy_api_with_space_after_comma() throws Exception {
        shouldDeployApiWithTags("test, !toto", new String[] { "test" });
    }

    @Test
    public void test_deploy_api_with_space_before_comma() throws Exception {
        shouldDeployApiWithTags("test ,!toto", new String[] { "test" });
    }

    @Test
    public void test_deploy_api_with_space_before_tag() throws Exception {
        shouldDeployApiWithTags(" test,!toto", new String[] { "test" });
    }

    @Test
    public void should_update_api() {
        final NativeApi api = buildTestApi();
        final NativePlan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        final NativeApi api2 = buildTestApi();
        Instant deployDateInst = api.getDeployedAt().toInstant().plus(Duration.ofHours(1));
        api2.setDeployedAt(Date.from(deployDateInst));
        api2.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api2);

        verify(eventManager).publishEvent(ReactorEvent.UPDATE, api2);
    }

    @Test
    public void should_not_update_api() {
        final NativeApi api = buildTestApi();
        final NativePlan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        final NativeApi api2 = buildTestApi();
        Instant deployDateInst = api.getDeployedAt().toInstant().minus(Duration.ofHours(1));
        api2.setDeployedAt(Date.from(deployDateInst));

        apiManager.register(api2);

        verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
    }

    @Test
    public void should_undeploy_api_no_more_matching_tag() {
        final NativeApi api = buildTestApi();
        final NativePlan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));
        api.getDefinition().setTags(new HashSet<>(singletonList("test")));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        final NativeApi api2 = buildTestApi();
        api2.setDeployedAt(new Date());
        api2.getDefinition().setTags(new HashSet<>(singletonList("other-tag")));

        apiManager.register(api2);

        verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
    }

    private NativePlan buildMockPlan() {
        NativePlan plan = mock(NativePlan.class);
        lenient().when(plan.getStatus()).thenReturn(PlanStatus.PUBLISHED);

        return plan;
    }

    @Test
    public void should_undeploy_api() {
        final NativeApi api = buildTestApi();
        final NativePlan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        apiManager.unregister(api.getId());

        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
    }

    @Test
    public void shouldNotUndeployUnknownApi() {
        final NativeApi api = buildTestApi();
        final NativePlan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        apiManager.unregister("unknown-api");

        verify(eventManager, never()).publishEvent(ReactorEvent.UNDEPLOY, api);
    }

    @Test
    public void should_undeploy_api_no_more_plan() {
        final NativeApi api = buildTestApi();
        final NativePlan mockedPlan = buildMockPlan();

        api.getDefinition().setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        final NativeApi api2 = buildTestApi();
        api2.setDeployedAt(new Date(api.getDeployedAt().getTime() + 100));
        api2.getDefinition().setPlans(Collections.emptyList());

        apiManager.register(api2);

        verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
    }

    @Test
    public void should_decrypt_api_properties_on_deployment() throws Exception {
        final NativeApi api = buildTestApi();

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
    public void should_not_deploy_if_license_is_invalid() {
        final NativeApi api = buildTestApi();
        final String orgId = api.getOrganizationId();
        doThrow(new InvalidLicenseException("Invalid license for test"))
            .when(licenseManager)
            .validatePluginFeatures(eq(orgId), anyCollection());
        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    @SneakyThrows
    public void should_not_deploy_if_api_uses_feature_not_entitled_by_license() {
        final NativeApi api = buildTestApi();
        final String orgId = api.getOrganizationId();
        doThrow(new ForbiddenFeatureException(List.of(new LicenseManager.ForbiddenFeature("Not entitled feature", "some plugin"))))
            .when(licenseManager)
            .validatePluginFeatures(eq(orgId), anyCollection());
        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    private NativeApi buildTestApi() {
        KafkaListener kafkaListener = new KafkaListener();
        kafkaListener.setHost("host");
        return new NativeApiBuilder()
            .id("api-test")
            .name("api-name-test")
            .listeners(List.of(kafkaListener))
            .deployedAt(new Date())
            .organizationId("org-id")
            .build();
    }

    class NativeApiBuilder {

        private final io.gravitee.definition.model.v4.nativeapi.NativeApi definition =
            new io.gravitee.definition.model.v4.nativeapi.NativeApi();
        private final NativeApi api = new NativeApi(definition);

        public NativeApiBuilder id(String id) {
            this.definition.setId(id);
            return this;
        }

        public NativeApiBuilder name(String name) {
            this.definition.setName(name);
            return this;
        }

        public NativeApiBuilder listeners(List<NativeListener> listeners) {
            this.definition.setListeners(listeners);
            return this;
        }

        public NativeApiBuilder deployedAt(Date updatedAt) {
            this.api.setDeployedAt(updatedAt);
            return this;
        }

        public NativeApiBuilder organizationId(String organizationId) {
            this.api.setOrganizationId(organizationId);
            return this;
        }

        public NativeApi build() {
            api.setEnabled(true);

            return this.api;
        }
    }
}
