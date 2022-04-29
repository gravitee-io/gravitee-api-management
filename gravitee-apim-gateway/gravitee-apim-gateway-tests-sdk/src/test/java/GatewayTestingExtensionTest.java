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
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;
import testcases.ClientAuthenticationPEMInlineTestCase;
import testcases.ConditionalPolicyTestCase;
import testcases.Http2HeadersTestCase;
import testcases.InvalidApiClassLevelTestCase;
import testcases.InvalidGatewayConfigFolderTestCase;
import testcases.NotExtendingAbstractClassTestCase;
import testcases.RegisterTwiceSameApiClassLevelTestCase;
import testcases.RegisterTwiceSameApiMethodLevelTestCase;
import testcases.SuccessTestCase;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
class GatewayTestingExtensionTest {

    @Test
    @DisplayName("Should success tests")
    void shouldSuccessAllTests() {
        EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(SuccessTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(
                stats -> {
                    stats.started(1).succeeded(1);
                }
            );
    }

    @Test
    @DisplayName("Should success tests in HTTP2 conf")
    void shouldSuccessAllHTTP2Tests() {
        EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(Http2HeadersTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(
                stats -> {
                    stats.started(2).succeeded(2);
                }
            );
    }

    /**
     * This test is useful to verify AbstractGatewayTest automatically updates the endpoints with the HTTPS port if needed
     */
    @Test
    @DisplayName("Should success tests with secured endpoints configuration")
    void shouldSuccessTestsWithSecuredBackends() {
        EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(ClientAuthenticationPEMInlineTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(
                stats -> {
                    stats.started(1).succeeded(1);
                }
            );
    }

    @Test
    @DisplayName("Should success tests with valid API and fail those with non existing API at method level")
    void shouldSuccessTestsWithValidAPIAndFailThoseWithNonValidOne() {
        EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(ConditionalPolicyTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(
                stats -> {
                    stats.started(3).succeeded(2).failed(1);
                }
            );
    }

    @Test
    @DisplayName("Should not start tests if importing non existing API at class level")
    void shouldNotStartTestIfImportingNonExistingApiAtClassLevel() {
        EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(InvalidApiClassLevelTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(
                stats -> {
                    stats.started(0);
                }
            );
    }

    @Test
    @DisplayName("Should not start tests if using non existing gateway configuration folder")
    void shouldNotStartUsingNonExistingGatewayConfiguration() {
        EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(InvalidGatewayConfigFolderTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(
                stats -> {
                    stats.started(0);
                }
            );
    }

    @Test
    @DisplayName("Should not start tests if trying to deploy an already deployed api at class level")
    void shouldFailIfTryingToDeployAnAlreadyDeployedApiAtClassLevel() {
        EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(RegisterTwiceSameApiClassLevelTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(
                stats -> {
                    stats.started(0);
                }
            );
    }

    @Test
    @DisplayName("Should fail test method if trying to deploy an already deployed api")
    void shouldFailIfTryingToDeployAnAlreadyDeployedApi() {
        EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(RegisterTwiceSameApiMethodLevelTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(
                stats -> {
                    stats.started(2).succeeded(1).failed(1);
                }
            );
    }

    @Test
    @DisplayName("Should not start tests if not extending AbstractGatewayTest")
    void shouldFailIfNotExtendingAbstractGatewayTest() {
        EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(NotExtendingAbstractClassTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(
                stats -> {
                    stats.started(0);
                }
            );
    }
}
