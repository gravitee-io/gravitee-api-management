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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
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
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.impl.ApiManagerImpl;
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
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class ApiManagerTest {

    @Mock
    private EventManager eventManager;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private DataEncryptor dataEncryptor;

    @Mock
    private LicenseManager licenseManager;

    private ApiManagerImpl apiManager;

    @BeforeEach
    public void setUp() throws Exception {
        apiManager = spy(new ApiManagerImpl(eventManager, gatewayConfiguration, licenseManager, dataEncryptor));
        lenient().when(gatewayConfiguration.shardingTags()).thenReturn(Optional.empty());
        lenient().when(gatewayConfiguration.hasMatchingTags(any())).thenCallRealMethod();
    }

    @Nested
    class Register {

        @Nested
        class Deploy {

            @Test
            public void should_not_deploy_disable_api() {
                final Api api = buildTestApi();
                api.setEnabled(false);

                apiManager.register(api);

                verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
            }

            @Test
            public void should_not_deploy_api_without_plan() {
                final Api api = buildTestApi();

                apiManager.register(api);

                verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
                assertThat(apiManager.apis()).isEmpty();
            }

            @Test
            public void should_not_deploy_api_when_same_tag_defined_in_both_include_and_exclude() {
                final Api api = buildTestApi();
                final Plan mockedPlan = mock(Plan.class);

                api.getDefinition().setPlans(singletonList(mockedPlan));
                api.getDefinition().setTags(new HashSet<>(List.of("test")));

                when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(List.of("test,!test")));

                apiManager.register(api);

                verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
            }

            @Test
            public void should_not_deploy_api_having_excluded_tag_despite_having_matching_tag() {
                final Api api = buildTestApi();
                final Plan mockedPlan = mock(Plan.class);

                api.getDefinition().setPlans(singletonList(mockedPlan));
                api.getDefinition().setTags(new HashSet<>(Arrays.asList("product", "international")));

                when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList("product", "!international")));

                apiManager.register(api);

                verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
            }

            @Test
            public void should_not_deploy_untagged_api_when_gateway_has_tag() {
                final Api api = buildTestApi();
                final Plan mockedPlan = mock(Plan.class);

                api.getDefinition().setPlans(singletonList(mockedPlan));

                when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("product")));

                apiManager.register(api);

                verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
            }

            @Test
            public void should_not_deploy_api_if_plan_has_non_matching_tag() {
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
                doThrow(new ForbiddenFeatureException(List.of(new LicenseManager.ForbiddenFeature("Not entitled feature", "some plugin"))))
                    .when(licenseManager)
                    .validatePluginFeatures(eq(orgId), anyCollection());
                apiManager.register(api);

                verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
            }

            @Test
            public void should_deploy_api_with_plan() {
                final Api api = buildTestApi();
                final Plan mockedPlan = aPlan();

                api.getDefinition().setPlans(singletonList(mockedPlan));

                apiManager.register(api);

                verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
                assertThat(apiManager.apis()).hasSize(1);
            }

            @Test
            public void should_deploy_api_with_tag_on_tagless_gateway() {
                final Api api = buildTestApi();
                final Plan mockedPlan = aPlan();

                api.getDefinition().setPlans(singletonList(mockedPlan));
                api.getDefinition().setTags(new HashSet<>(singletonList("test")));

                apiManager.register(api);

                verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
            }

            @Test
            public void should_deploy_api_with_plan_matching_tag() {
                final Api api = buildTestApi();
                api.getDefinition().setTags(new HashSet<>(singletonList("test")));
                api.getDefinition().setPlans(singletonList(aPlan().toBuilder().tags(new HashSet<>(singletonList("test"))).build()));

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
                final Api api = buildTestApi();
                final Plan mockedPlan = aPlan();

                api.getDefinition().setPlans(singletonList(mockedPlan));
                api.getDefinition().setTags(new HashSet<>(singletonList(apiTag)));

                when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList(gatewayTags.split(","))));

                apiManager.register(api);

                verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
            }
        }

        @Nested
        class Update {

            @Test
            public void should_update_api() {
                final Api api = buildTestApi();
                final Plan mockedPlan = aPlan();
                api.setRevision("rev1");
                api.getDefinition().setPlans(singletonList(mockedPlan));

                apiManager.register(api);

                verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                final Api api2 = buildTestApi();
                Instant deployDateInst = api.getDeployedAt().toInstant().plus(Duration.ofHours(1));
                api2.setDeployedAt(Date.from(deployDateInst));
                api2.setRevision("rev2");
                api2.getDefinition().setPlans(singletonList(mockedPlan));

                apiManager.register(api2);

                verify(eventManager).publishEvent(ReactorEvent.UPDATE, api2);
            }

            @Test
            public void should_not_update_api_when_deployment_date_is_older() {
                final Api api = buildTestApi();
                final Plan mockedPlan = aPlan();
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
            public void should_not_update_api_when_revision_is_the_same() {
                final Api api = buildTestApi();
                final Plan mockedPlan = aPlan();
                api.setRevision("rev1");
                api.getDefinition().setPlans(singletonList(mockedPlan));

                apiManager.register(api);
                verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                final Api api2 = buildTestApi();
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
                final Api api = buildTestApi();
                api.getDefinition().setPlans(singletonList(aPlan()));
                apiManager.register(api);
                verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                apiManager.unregister(api.getId());

                verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
            }

            @Test
            public void should_undeploy_api_when_tag_no_longer_matches_gateway_configuration() {
                final Api api = buildTestApi();
                api.setRevision("rev1");
                api.getDefinition().setPlans(singletonList(aPlan()));
                api.getDefinition().setTags(new HashSet<>(singletonList("test")));

                when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));
                apiManager.register(api);
                verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                final Api api2 = buildTestApi();
                api2.setRevision("rev2");
                api2.setDeployedAt(new Date());
                api2.getDefinition().setTags(new HashSet<>(singletonList("other-tag")));

                apiManager.register(api2);

                verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
                verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
            }

            @Test
            public void should_not_undeploy_unknown_api() {
                final Api api = buildTestApi();
                api.getDefinition().setPlans(singletonList(aPlan()));
                apiManager.register(api);
                verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                apiManager.unregister("unknown-api");
                verify(eventManager, never()).publishEvent(ReactorEvent.UNDEPLOY, api);
            }

            @Test
            public void should_undeploy_api_no_more_plan() {
                final Api api = buildTestApi();
                api.setRevision("rev1");
                api.getDefinition().setPlans(singletonList(aPlan()));
                apiManager.register(api);
                verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                final Api api2 = buildTestApi();
                api2.setDeployedAt(new Date(api.getDeployedAt().getTime() + 100));
                api2.setRevision("rev2");
                api2.getDefinition().setPlans(Collections.emptyList());
                apiManager.register(api2);

                verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
                verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
            }

            @Test
            public void should_undeploy_disabled_api() {
                var plans = singletonList(aPlan());

                final Api api = buildTestApi();
                api.setRevision("rev1");
                api.getDefinition().setPlans(plans);
                apiManager.register(api);
                verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

                final Api api2 = buildTestApi();
                api2.getDefinition().setPlans(plans);
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
            assertThat(api.getDefinition().getProperties().getValues()).containsOnly(
                Map.entry("key1", "plain value 1"),
                Map.entry("key2", "plain value 2"),
                Map.entry("key3", "plain value 3")
            );
        }
    }

    @Nested
    class RequiredActionFor {

        @Test
        public void should_require_deployment_when_api_is_not_deployed() {
            final Api api = buildTestApi();
            api.getDefinition().setPlans(singletonList(aPlan()));

            ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
            assertThat(actionOnApi).isEqualTo(ActionOnApi.DEPLOY);
        }

        @Test
        public void should_require_deployment_with_updated_api() {
            List<Plan> plans = singletonList(aPlan());

            final Api api = buildTestApi();
            api.setRevision("rev1");
            api.getDefinition().setPlans(plans);
            apiManager.register(api);

            ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
            assertThat(actionOnApi).isEqualTo(ActionOnApi.NONE);

            final Api api2 = buildTestApi();
            Instant deployDateInst = api.getDeployedAt().toInstant().plus(Duration.ofHours(1));
            api2.setDeployedAt(Date.from(deployDateInst));
            api2.setRevision("rev2");
            api2.getDefinition().setPlans(plans);

            ActionOnApi actionOnApi2 = apiManager.requiredActionFor(api2);
            assertThat(actionOnApi2).isEqualTo(ActionOnApi.DEPLOY);
        }

        @Test
        public void should_require_deployment_with_matching_tag() {
            final Api api = buildTestApi();
            api.getDefinition().setTags(new HashSet<>(singletonList("test")));
            when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

            ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
            assertThat(actionOnApi).isEqualTo(ActionOnApi.DEPLOY);
        }

        @Test
        public void should_not_require_deployment_with_same_api() {
            final Api api = buildTestApi();
            api.getDefinition().setPlans(singletonList(aPlan()));

            apiManager.register(api);
            ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
            assertThat(actionOnApi).isEqualTo(ActionOnApi.NONE);
        }

        @Test
        public void should_not_require_deployment_with_same_revision() {
            List<Plan> plans = singletonList(aPlan());
            final Api api = buildTestApi();
            api.getDefinition().setPlans(plans);
            api.setRevision("rev1");
            apiManager.register(api);

            final Api api2 = buildTestApi();
            Instant deployDateInst = api.getDeployedAt().toInstant().plus(Duration.ofHours(1));
            api2.setDeployedAt(Date.from(deployDateInst));
            api2.setRevision("rev1");
            api2.getDefinition().setPlans(plans);

            ActionOnApi actionOnApi2 = apiManager.requiredActionFor(api2);
            assertThat(actionOnApi2).isEqualTo(ActionOnApi.NONE);
        }

        @Test
        public void should_not_require_deployment_without_matching_tag_and_api_not_already_deployed() {
            final Api api = buildTestApi();
            api.getDefinition().setTags(new HashSet<>(singletonList("test2")));
            when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

            ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
            assertThat(actionOnApi).isEqualTo(ActionOnApi.NONE);
        }

        @Test
        public void should_require_undeployment_without_matching_tag_and_api_already_deployed() {
            final Api api = buildTestApi();
            api.getDefinition().setPlans(singletonList(aPlan()));
            apiManager.register(api);

            api.getDefinition().setTags(new HashSet<>(singletonList("test2")));
            when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

            ActionOnApi actionOnApi = apiManager.requiredActionFor(api);
            assertThat(actionOnApi).isEqualTo(ActionOnApi.UNDEPLOY);
        }
    }

    private Api buildTestApi() {
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(singletonList(mock(VirtualHost.class)));
        return new ApiBuilder().id("api-test").name("api-name-test").proxy(proxy).deployedAt(new Date()).build();
    }

    private Plan aPlan() {
        return Plan.builder().status("published").build();
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
