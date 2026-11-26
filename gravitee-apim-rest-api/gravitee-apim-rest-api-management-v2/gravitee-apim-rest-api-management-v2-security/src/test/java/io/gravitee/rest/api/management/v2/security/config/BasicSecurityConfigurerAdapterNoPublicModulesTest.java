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
package io.gravitee.rest.api.management.v2.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.security.config.AbstractSecurityConfigurerAdapterTest;
import io.gravitee.rest.api.service.EnvironmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ContextConfiguration(classes = { BasicSecurityConfigurerAdapter.class, AbstractSecurityConfigurerAdapterTest.TestConfig.class })
@TestPropertySource(properties = "gamma.modules.public-endpoints=")
class BasicSecurityConfigurerAdapterNoPublicModulesTest extends AbstractSecurityConfigurerAdapterTest {

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void resolveEnvironment() {
        var environment = new EnvironmentEntity();
        environment.setId("DEFAULT");
        environment.setOrganizationId("DEFAULT");
        when(environmentService.findByOrgAndIdOrHrid(any(), any())).thenReturn(environment);
    }

    @Override
    protected String getPath() {
        return "/openapi-v2.yaml";
    }

    @Test
    void should_not_permit_any_module_public_endpoint_when_none_opted_in() throws Exception {
        int status = mockMvc
            .perform(get("/organizations/DEFAULT/environments/DEFAULT/modules/singularitee/public/_sync/release"))
            .andReturn()
            .getResponse()
            .getStatus();

        assertThat(status).isEqualTo(401);
    }
}
