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
package io.gravitee.gamma.module.platform.infra.service_provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnectionRepository;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AmSdkClientFactoryTest {

    private Vertx vertx;
    private AmSdkClientFactory factory;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        factory = new AmSdkClientFactory(vertx, mock(AmConnectionRepository.class));
    }

    @AfterEach
    void tearDown() {
        vertx.close();
    }

    @Test
    void should_carry_the_am_organization_from_the_connection() {
        var apis = factory.forConnection(connection("am-org-9"));

        assertThat(apis.amOrganizationId()).isEqualTo("am-org-9");
    }

    @Test
    void should_fall_back_to_the_default_organization_when_the_connection_has_none() {
        assertThat(factory.forConnection(connection(null)).amOrganizationId()).isEqualTo("DEFAULT");
        assertThat(factory.forConnection(connection("  ")).amOrganizationId()).isEqualTo("DEFAULT");
    }

    private static AmConnection connection(String amOrganizationId) {
        return new AmConnection("http://am:8093", "token", amOrganizationId, "env-7", "domain-1", "domain-hrid", null);
    }
}
