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

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;
import testcases.*;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GatewayTestingExtensionTest {

    /**
     * Setting this property allow to run test cases only in the context of the extension testing.
     * *TestCase.java classes are not aimed to be run when running all tests in your IDE.
     */
    @BeforeEach
    void setUp() {
        System.setProperty("io.gravitee.sdk.testcase.enabled", "true");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("io.gravitee.sdk.testcase.enabled");
    }

    @Test
    void should_success_all_tests() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(SuccessTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(1).succeeded(1));
    }

    @Test
    void should_replace_placeholders() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(PlaceholderTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(1).succeeded(1));
    }

    @Test
    void should_redeploy_tests() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(ManuallyRedeployTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(4).succeeded(4));
    }

    @Test
    void should_undeploy_tests() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(ManuallyUndeployTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(1).succeeded(1));
    }

    @Test
    void should_deploy_class_level_tests() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(ManuallyDeployTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(2).failed(2));
    }

    @Test
    void should_success_all_http2_tests() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(Http2HeadersTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(2).succeeded(2));
    }

    /**
     * This test is useful to verify AbstractGatewayTest automatically updates the endpoints with the HTTPS port if needed
     */
    @Test
    void should_success_tests_with_secured_backends() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(ClientAuthenticationPEMInlineTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(1).succeeded(1));
    }

    @Test
    void should_success_tests_with_valid_api_and_fail_those_with_non_validone() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(ConditionalPolicyTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.succeeded(2).failed(1));
    }

    @Test
    void should_not_start_test_if_importing_non_existing_api_at_class_level() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(InvalidApiClassLevelTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(0));
    }

    @Test
    void should_not_start_using_non_existing_gateway_configuration() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(InvalidGatewayConfigFolderTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(0));
    }

    @Test
    void should_fail_if_trying_to_deploy_an_already_deployed_api_at_class_level() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(RegisterTwiceSameApiClassLevelTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(0));
    }

    @Test
    void should_fail_if_trying_to_deploy_an_already_deployed_api() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(RegisterTwiceSameApiMethodLevelTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(2).succeeded(1).failed(1));
    }

    @Test
    void should_fail_if_not_extending_abstract_gateway_test() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(NotExtendingAbstractClassTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(0));
    }

    @Test
    void should_success_tests_with_organization() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(OrganizationDeploymentTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(4).succeeded(3).failed(1));
    }

    @Test
    void should_success_grpc_test() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(GrpcTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    }

    @Test
    void should_start_gateway_and_apis_with_selected_mode() {
        EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(GatewayModeTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(6).succeeded(6).aborted(0).skipped(0).failed(0));
    }
}
