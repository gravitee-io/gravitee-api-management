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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.event.EventManager;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.nativeapi.NativeListener;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.impl.ApiManagerImpl;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.reactive.handlers.api.v4.NativeApi;
import io.gravitee.gateway.reactor.ReactableApi;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApiManagerImplTest {

    /**
     * Abstract base class containing all common test logic
     */
    public abstract static class AbstractApiManagerTest {

        @Mock
        protected EventManager eventManager;

        @Mock
        protected GatewayConfiguration gatewayConfiguration;

        @Mock
        protected DataEncryptor dataEncryptor;

        @Mock
        protected LicenseManager licenseManager;

        protected ApiManagerImpl apiManager;

        @BeforeEach
        public void setUp() throws Exception {
            apiManager = spy(new ApiManagerImpl(eventManager, gatewayConfiguration, licenseManager, dataEncryptor));
            lenient().when(gatewayConfiguration.shardingTags()).thenReturn(Optional.empty());
            lenient().when(gatewayConfiguration.hasMatchingTags(any())).thenCallRealMethod();
        }

        protected abstract ReactableApi<?> buildTestApi();

        protected abstract Object buildMockPlan();

        @Nested
        class Register {

            @Nested
            class Deploy {

                @Test
                public void should_not_deploy_disable_api() {
                    var api = buildTestApi();
                    api.setEnabled(false);

                    apiManager.register(api);

                    verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
                }

                @Test
                public void should_not_deploy_api_without_plan() {
                    var api = buildTestApi();

                    apiManager.register(api);

                    verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
                    assertThat(apiManager.apis()).isEmpty();
                }

                @Test
                public void should_not_deploy_api_when_same_tag_defined_in_both_include_and_exclude() {
                    var api = buildTestApi();
                    var plan = buildMockPlan();

                    setPlans(api, singletonList(plan));
                    setTags(api, new HashSet<>(List.of("test")));

                    when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(List.of("test,!test")));

                    apiManager.register(api);

                    verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
                }

                @Test
                public void should_not_deploy_api_having_excluded_tag_despite_having_matching_tag() {
                    var api = buildTestApi();
                    var plan = buildMockPlan();

                    setPlans(api, singletonList(plan));
                    setTags(api, new HashSet<>(Arrays.asList("product", "international")));

                    when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList("product", "!international")));

                    apiManager.register(api);

                    verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
                }

                @Test
                public void should_not_deploy_untagged_api_when_gateway_has_tag() {
                    var api = buildTestApi();
                    var plan = buildMockPlan();

                    setPlans(api, singletonList(plan));

                    when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("product")));

                    apiManager.register(api);

                    verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
                }

                @Test
                public void should_not_deploy_api_if_plan_has_non_matching_tag() {
                    var api = buildTestApi();
                    setTags(api, new HashSet<>(singletonList("test")));

                    var plan = buildMockPlan();
                    setPlanTags(plan, new HashSet<>(singletonList("test2")));
                    setPlans(api, singletonList(plan));

                    when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

                    apiManager.register(api);

                    verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
                }

                @Test
                @SneakyThrows
                public void should_not_deploy_if_license_is_invalid() {
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
                public void should_not_deploy_if_api_uses_feature_not_entitled_by_license() {
                    var api = buildTestApi();
                    final String orgId = api.getOrganizationId();
                    doThrow(
                        new ForbiddenFeatureException(List.of(new LicenseManager.ForbiddenFeature("Not entitled feature", "some plugin")))
                    )
                        .when(licenseManager)
                        .validatePluginFeatures(eq(orgId), anyCollection());
                    apiManager.register(api);

                    verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
                }

                @Test
                public void should_deploy_api_with_plan() {
                    var api = buildTestApi();
                    var plan = buildMockPlan();

                    setPlans(api, singletonList(plan));

                    apiManager.register(api);

                    verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
                    assertThat(apiManager.apis()).hasSize(1);
                }

                @Test
                public void should_deploy_api_with_tag_on_tagless_gateway() {
                    var api = buildTestApi();
                    var plan = buildMockPlan();

                    setPlans(api, singletonList(plan));
                    setTags(api, new HashSet<>(singletonList("test")));

                    apiManager.register(api);

                    verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
                }

                @Test
                public void should_deploy_api_with_plan_matching_tag() {
                    var api = buildTestApi();
                    setTags(api, new HashSet<>(singletonList("test")));
                    var plan = buildMockPlan();
                    setPlanTags(plan, new HashSet<>(singletonList("test")));
                    setPlans(api, singletonList(plan));

                    when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

                    apiManager.register(api);

                    verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
                }

                @ParameterizedTest(name = "{2}")
                @CsvSource(
                    delimiter = '|',
                    textBlock = """
                    !test       |  toto    |   test_deployApiWithTagExcluded
                    test,toto   |  test    |   test_deployApiWithTag
                    test,toto   |  Test    |   test_deployApiWithUpperCasedTag
                    test,toto   |  tést    |   test_deployApiWithAccentTag
                    test        |  Tést    |   test_deployApiWithUpperCasedAndAccentTag
                    test,!toto  |  test    |   test_deployApiWithTagExclusion
                    test, !toto |  test    |   test_deployApiWithSpaceAfterComma
                    test ,!toto |  test    |   test_deployApiWithSpaceBeforeComma
                     test,!toto |  test    |   test_deployApiWithSpaceBeforeTag
                    """
                )
                void should_deploy_api_with_different_tag_combinations(String gatewayTags, String apiTag, String label) {
                    var api = buildTestApi();
                    var plan = buildMockPlan();

                    setPlans(api, singletonList(plan));
                    setTags(api, new HashSet<>(singletonList(apiTag)));

                    when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList(gatewayTags.split(","))));

                    apiManager.register(api);

                    verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
                }
            }

            @Nested
            class Update {

                @Test
                public void should_update_api() {
                    var api = buildTestApi();
                    var plan = buildMockPlan();
                    api.setRevision("rev1");
                    setPlans(api, singletonList(plan));

                    apiManager.register(api);

                    verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                    var api2 = buildTestApi();
                    Instant deployDateInst = api.getDeployedAt().toInstant().plus(Duration.ofHours(1));
                    api2.setDeployedAt(Date.from(deployDateInst));
                    api2.setRevision("rev2");
                    setPlans(api2, singletonList(plan));

                    apiManager.register(api2);

                    verify(eventManager).publishEvent(ReactorEvent.UPDATE, api2);
                }

                @Test
                public void should_not_update_api_when_deployment_date_is_older() {
                    var api = buildTestApi();
                    var plan = buildMockPlan();
                    setPlans(api, singletonList(plan));

                    apiManager.register(api);
                    verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                    var api2 = buildTestApi();
                    Instant deployDateInst = api.getDeployedAt().toInstant().minus(Duration.ofHours(1));
                    api2.setDeployedAt(Date.from(deployDateInst));

                    apiManager.register(api2);

                    verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
                }

                @Test
                public void should_not_update_api_when_revision_is_the_same() {
                    var api = buildTestApi();
                    var plan = buildMockPlan();
                    api.setRevision("rev1");
                    setPlans(api, singletonList(plan));

                    apiManager.register(api);
                    verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                    var api2 = buildTestApi();
                    api2.setRevision("rev1");
                    Instant deployDateInst = api.getDeployedAt().toInstant().minus(Duration.ofHours(1));
                    api2.setDeployedAt(Date.from(deployDateInst));

                    apiManager.register(api2);

                    verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
                }
            }

            @Nested
            class Undeploy {

                @Test
                public void should_undeploy_api() {
                    var api = buildTestApi();
                    setPlans(api, singletonList(buildMockPlan()));
                    apiManager.register(api);
                    verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                    apiManager.unregister(api.getId());

                    verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
                }

                @Test
                public void should_undeploy_api_when_tag_no_longer_matches_gateway_configuration() {
                    var api = buildTestApi();
                    api.setRevision("rev1");
                    setPlans(api, singletonList(buildMockPlan()));
                    setTags(api, new HashSet<>(singletonList("test")));

                    when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));
                    apiManager.register(api);
                    verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                    var api2 = buildTestApi();
                    api2.setRevision("rev2");
                    api2.setDeployedAt(new Date());
                    setTags(api2, new HashSet<>(singletonList("other-tag")));

                    apiManager.register(api2);

                    verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
                    verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
                }

                @Test
                public void should_not_undeploy_unknown_api() {
                    var api = buildTestApi();
                    setPlans(api, singletonList(buildMockPlan()));
                    apiManager.register(api);
                    verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                    apiManager.unregister("unknown-api");
                    verify(eventManager, never()).publishEvent(ReactorEvent.UNDEPLOY, api);
                }

                @Test
                public void should_undeploy_api_no_more_plan() {
                    var api = buildTestApi();
                    api.setRevision("rev1");
                    setPlans(api, singletonList(buildMockPlan()));
                    apiManager.register(api);
                    verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                    var api2 = buildTestApi();
                    api2.setDeployedAt(new Date(api.getDeployedAt().getTime() + 100));
                    api2.setRevision("rev2");
                    setPlans(api2, Collections.emptyList());
                    apiManager.register(api2);

                    verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
                    verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
                }

                @Test
                public void should_undeploy_disabled_api() {
                    var plans = singletonList(buildMockPlan());

                    var api = buildTestApi();
                    api.setRevision("rev1");
                    setPlans(api, plans);
                    apiManager.register(api);
                    verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                    var api2 = buildTestApi();
                    setPlans(api2, plans);
                    api2.setRevision("rev2");
                    api2.setDeployedAt(new Date(api.getDeployedAt().getTime() + 100));
                    api2.setEnabled(false);

                    apiManager.register(api2);

                    verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
                    verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
                }
            }

            @Test
            public void should_decrypt_api_properties_on_deployment() throws Exception {
                var api = buildTestApi();

                setProperties(api);

                when(dataEncryptor.decrypt("value2Base64encrypted")).thenReturn("plain value 2");
                when(dataEncryptor.decrypt("value3Base64encrypted")).thenReturn("plain value 3");

                apiManager.register(api);

                verify(dataEncryptor, times(2)).decrypt(any());
                verifyPropertiesDecrypted(api);
            }
        }

        @Nested
        class RequiredActionFor {

            @Test
            public void should_require_deployment_when_api_is_not_deployed() {
                var api = buildTestApi();
                setPlans(api, singletonList(buildMockPlan()));

                ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
                assertThat(actionOnApi).isEqualTo(ActionOnApi.DEPLOY);
            }

            @Test
            public void should_require_deployment_with_updated_api() {
                List<Object> plans = singletonList(buildMockPlan());

                var api = buildTestApi();
                api.setRevision("rev1");
                setPlans(api, plans);
                apiManager.register(api);

                ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
                assertThat(actionOnApi).isEqualTo(ActionOnApi.NONE);

                var api2 = buildTestApi();
                Instant deployDateInst = api.getDeployedAt().toInstant().plus(Duration.ofHours(1));
                api2.setDeployedAt(Date.from(deployDateInst));
                api2.setRevision("rev2");
                setPlans(api2, plans);

                ActionOnApi actionOnApi2 = apiManager.requiredActionFor(api2);
                assertThat(actionOnApi2).isEqualTo(ActionOnApi.DEPLOY);
            }

            @Test
            public void should_require_deployment_with_matching_tag() {
                var api = buildTestApi();
                setTags(api, new HashSet<>(singletonList("test")));
                when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

                ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
                assertThat(actionOnApi).isEqualTo(ActionOnApi.DEPLOY);
            }

            @Test
            public void should_not_require_deployment_with_same_api() {
                var api = buildTestApi();
                setPlans(api, singletonList(buildMockPlan()));

                apiManager.register(api);
                ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
                assertThat(actionOnApi).isEqualTo(ActionOnApi.NONE);
            }

            @Test
            public void should_not_require_deployment_with_same_revision() {
                List<Object> plans = singletonList(buildMockPlan());
                var api = buildTestApi();
                setPlans(api, plans);
                api.setRevision("rev1");
                apiManager.register(api);

                var api2 = buildTestApi();
                Instant deployDateInst = api.getDeployedAt().toInstant().plus(Duration.ofHours(1));
                api2.setDeployedAt(Date.from(deployDateInst));
                api2.setRevision("rev1");
                setPlans(api2, plans);

                ActionOnApi actionOnApi2 = apiManager.requiredActionFor(api2);
                assertThat(actionOnApi2).isEqualTo(ActionOnApi.NONE);
            }

            @Test
            public void should_not_require_deployment_without_matching_tag_and_api_not_already_deployed() {
                var api = buildTestApi();
                setTags(api, new HashSet<>(singletonList("test2")));
                when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

                ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
                assertThat(actionOnApi).isEqualTo(ActionOnApi.NONE);
            }

            @Test
            public void should_require_undeployment_without_matching_tag_and_api_already_deployed() {
                var api = buildTestApi();
                setPlans(api, singletonList(buildMockPlan()));
                apiManager.register(api);

                setTags(api, new HashSet<>(singletonList("test2")));
                when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

                ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
                assertThat(actionOnApi).isEqualTo(ActionOnApi.UNDEPLOY);
            }
        }

        // Helper methods for API manipulation
        protected abstract void setPlans(ReactableApi<?> api, List<Object> plans);

        protected abstract void setTags(ReactableApi<?> api, HashSet<String> tags);

        protected abstract void setPlanTags(Object plan, HashSet<String> tags);

        protected abstract void setProperties(ReactableApi<?> api);

        protected abstract void verifyPropertiesDecrypted(ReactableApi<?> api);
    }

    /**
     * V2 API Test Implementation
     */
    @Nested
    class V2ApiTest extends AbstractApiManagerTest {

        @Override
        protected ReactableApi<?> buildTestApi() {
            Proxy proxy = new Proxy();
            proxy.setVirtualHosts(singletonList(mock(VirtualHost.class)));
            return new ApiBuilder().id("api-test").name("api-name-test").proxy(proxy).deployedAt(new Date()).build();
        }

        @Override
        protected Object buildMockPlan() {
            return Plan.builder().id("plan-test").name("TestPlan").status("published").build();
        }

        @Override
        protected void setPlans(ReactableApi<?> api, List<Object> plans) {
            ((Api) api).getDefinition().setPlans((List<Plan>) (List<?>) plans);
        }

        @Override
        protected void setTags(ReactableApi<?> api, HashSet<String> tags) {
            ((Api) api).getDefinition().setTags(tags);
        }

        @Override
        protected void setPlanTags(Object plan, HashSet<String> tags) {
            ((Plan) plan).setTags(tags);
        }

        @Override
        protected void setProperties(ReactableApi<?> api) {
            Properties properties = new Properties();
            properties.setProperties(
                List.of(
                    new Property("key1", "plain value 1", false),
                    new Property("key2", "value2Base64encrypted", true),
                    new Property("key3", "value3Base64encrypted", true)
                )
            );
            ((Api) api).getDefinition().setProperties(properties);
        }

        @Override
        protected void verifyPropertiesDecrypted(ReactableApi<?> api) {
            assertThat(((Api) api).getDefinition().getProperties().getValues()).containsOnly(
                Map.entry("key1", "plain value 1"),
                Map.entry("key2", "plain value 2"),
                Map.entry("key3", "plain value 3")
            );
        }

        static class ApiBuilder {

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

    /**
     * V4 API Test Implementation
     */
    @Nested
    class V4ApiTest extends AbstractApiManagerTest {

        @Override
        protected ReactableApi<?> buildTestApi() {
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

        @Override
        protected Object buildMockPlan() {
            return io.gravitee.definition.model.v4.plan.Plan.builder()
                .id("plan-test")
                .name("TestPlan")
                .status(PlanStatus.PUBLISHED)
                .build();
        }

        @Override
        protected void setPlans(ReactableApi<?> api, List<Object> plans) {
            ((io.gravitee.gateway.reactive.handlers.api.v4.Api) api).getDefinition().setPlans(
                (List<io.gravitee.definition.model.v4.plan.Plan>) (List<?>) plans
            );
        }

        @Override
        protected void setTags(ReactableApi<?> api, HashSet<String> tags) {
            ((io.gravitee.gateway.reactive.handlers.api.v4.Api) api).getDefinition().setTags(tags);
        }

        @Override
        protected void setPlanTags(Object plan, HashSet<String> tags) {
            ((io.gravitee.definition.model.v4.plan.Plan) plan).setTags(tags);
        }

        @Override
        protected void setProperties(ReactableApi<?> api) {
            ((io.gravitee.gateway.reactive.handlers.api.v4.Api) api).getDefinition().setProperties(
                List.of(
                    new io.gravitee.definition.model.v4.property.Property("key1", "plain value 1", false),
                    new io.gravitee.definition.model.v4.property.Property("key2", "value2Base64encrypted", true),
                    new io.gravitee.definition.model.v4.property.Property("key3", "value3Base64encrypted", true)
                )
            );
        }

        @Override
        protected void verifyPropertiesDecrypted(ReactableApi<?> api) {
            assertThat(
                ((io.gravitee.gateway.reactive.handlers.api.v4.Api) api).getDefinition()
                    .getProperties()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            io.gravitee.definition.model.v4.property.Property::getKey,
                            io.gravitee.definition.model.v4.property.Property::getValue
                        )
                    )
            ).containsOnly(Map.entry("key1", "plain value 1"), Map.entry("key2", "plain value 2"), Map.entry("key3", "plain value 3"));
        }

        static class ApiBuilder {

            private final io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
            private final io.gravitee.gateway.reactive.handlers.api.v4.Api api = new io.gravitee.gateway.reactive.handlers.api.v4.Api(
                definition
            );

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

            public io.gravitee.gateway.reactive.handlers.api.v4.Api build() {
                api.setEnabled(true);
                return this.api;
            }
        }
    }

    @Nested
    class V4ApiWithApiProductTest extends V4ApiTest {

        @Mock
        private ApiProductRegistry apiProductRegistry;

        @Override
        @BeforeEach
        public void setUp() {
            apiManager = spy(new ApiManagerImpl(eventManager, gatewayConfiguration, licenseManager, dataEncryptor, apiProductRegistry));
            lenient().when(gatewayConfiguration.shardingTags()).thenReturn(Optional.empty());
            lenient().when(gatewayConfiguration.hasMatchingTags(any())).thenCallRealMethod();
        }

        @Nested
        class Register {

            @Test
            public void should_deploy_api_without_plan_when_included_in_api_product() {
                var api = buildTestApi();
                api.setEnvironmentId("env-1");
                setPlans(api, Collections.emptyList());

                when(apiProductRegistry.getApiProductPlanEntriesForApi(eq("api-test"), eq("env-1"))).thenReturn(
                    List.of(
                        new ApiProductRegistry.ApiProductPlanEntry("product-1", (io.gravitee.definition.model.v4.plan.Plan) buildMockPlan())
                    )
                );

                apiManager.register(api);

                verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
                assertThat(apiManager.apis()).hasSize(1);
            }

            @Test
            public void should_not_deploy_api_without_plan_when_not_included_in_api_product() {
                var api = buildTestApi();
                api.setEnvironmentId("env-1");
                setPlans(api, Collections.emptyList());

                when(apiProductRegistry.getApiProductPlanEntriesForApi(eq("api-test"), eq("env-1"))).thenReturn(List.of());

                apiManager.register(api);

                verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
                assertThat(apiManager.apis()).isEmpty();
            }
        }

        @Nested
        class Update {

            @Test
            public void should_keep_api_deployed_when_no_plans_but_included_in_api_product() {
                var api = buildTestApi();
                api.setRevision("rev1");
                api.setEnvironmentId("env-1");
                setPlans(api, singletonList(buildMockPlan()));

                apiManager.register(api);
                verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                var api2 = buildTestApi();
                api2.setRevision("rev2");
                api2.setEnvironmentId("env-1");
                api2.setDeployedAt(new Date(api.getDeployedAt().getTime() + 100));
                setPlans(api2, Collections.emptyList());

                when(apiProductRegistry.getApiProductPlanEntriesForApi(eq("api-test"), eq("env-1"))).thenReturn(
                    List.of(
                        new ApiProductRegistry.ApiProductPlanEntry("product-1", (io.gravitee.definition.model.v4.plan.Plan) buildMockPlan())
                    )
                );

                apiManager.register(api2);

                verify(eventManager).publishEvent(ReactorEvent.UPDATE, api2);
                verify(eventManager, never()).publishEvent(ReactorEvent.UNDEPLOY, api);
                assertThat(apiManager.apis()).hasSize(1);
            }
        }

        @Nested
        class Undeploy {

            @Test
            public void should_undeploy_api_when_no_plans_and_not_included_in_api_product() {
                var api = buildTestApi();
                api.setRevision("rev1");
                api.setEnvironmentId("env-1");
                setPlans(api, singletonList(buildMockPlan()));
                apiManager.register(api);
                verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                var api2 = buildTestApi();
                api2.setRevision("rev2");
                api2.setEnvironmentId("env-1");
                api2.setDeployedAt(new Date(api.getDeployedAt().getTime() + 100));
                setPlans(api2, Collections.emptyList());
                when(apiProductRegistry.getApiProductPlanEntriesForApi(eq("api-test"), eq("env-1"))).thenReturn(List.of());

                apiManager.register(api2);

                verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api2);
                verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
            }
        }
    }

    /**
     * Native API Test Implementation
     */
    @Nested
    class NativeApiTest extends AbstractApiManagerTest {

        @Override
        protected ReactableApi<?> buildTestApi() {
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

        @Override
        protected Object buildMockPlan() {
            return NativePlan.builder().id("plan-test").name("TestPlan").status(PlanStatus.PUBLISHED).build();
        }

        @Override
        protected void setPlans(ReactableApi<?> api, List<Object> plans) {
            ((NativeApi) api).getDefinition().setPlans((List<NativePlan>) (List<?>) plans);
        }

        @Override
        protected void setTags(ReactableApi<?> api, HashSet<String> tags) {
            ((NativeApi) api).getDefinition().setTags(tags);
        }

        @Override
        protected void setPlanTags(Object plan, HashSet<String> tags) {
            ((NativePlan) plan).setTags(tags);
        }

        @Override
        protected void setProperties(ReactableApi<?> api) {
            ((NativeApi) api).getDefinition().setProperties(
                List.of(
                    new io.gravitee.definition.model.v4.property.Property("key1", "plain value 1", false),
                    new io.gravitee.definition.model.v4.property.Property("key2", "value2Base64encrypted", true),
                    new io.gravitee.definition.model.v4.property.Property("key3", "value3Base64encrypted", true)
                )
            );
        }

        @Override
        protected void verifyPropertiesDecrypted(ReactableApi<?> api) {
            assertThat(
                ((NativeApi) api).getDefinition()
                    .getProperties()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            io.gravitee.definition.model.v4.property.Property::getKey,
                            io.gravitee.definition.model.v4.property.Property::getValue
                        )
                    )
            ).containsOnly(Map.entry("key1", "plain value 1"), Map.entry("key2", "plain value 2"), Map.entry("key3", "plain value 3"));
        }

        static class NativeApiBuilder {

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
}
