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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ContextConfiguration(
    classes = {
        BasicSecurityConfigurerAdapter.class,
        AbstractSecurityConfigurerAdapterTest.TestConfig.class,
        BasicSecurityConfigurerAdapterTest.DummyController.class,
    }
)
@TestPropertySource(properties = "gamma.modules.public-endpoints=singularitee, aihub")
class BasicSecurityConfigurerAdapterTest extends AbstractSecurityConfigurerAdapterTest {

    @Autowired
    private BasicSecurityConfigurerAdapter adapter;

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

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/organizations/DEFAULT/environments/DEFAULT/modules/singularitee/public/_sync/release",
            "/organizations/DEFAULT/environments/DEFAULT/modules/aihub/public/me",
            "/organizations/DEFAULT/environments/DEFAULT/modules/singularitee/public",
        }
    )
    void should_permit_public_endpoints_of_opted_in_modules(String path) throws Exception {
        int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/organizations/DEFAULT/environments/DEFAULT/modules/singularitee/groups/modules/models/public",
            "/organizations/O/environments/E/apis/modules/plans/public",
            "/organizations/DEFAULT/environments/DEFAULT/apis/x/modules/singularitee/public/y",
            "/organizations/DEFAULT/foo/environments/DEFAULT/modules/singularitee/public/y",
            "/organizations/DEFAULT/environments/DEFAULT/modules/other/public/_sync/release",
            "/organizations/DEFAULT/environments/DEFAULT/modules/singularitee-evil/public/x",
            "/organizations/DEFAULT/environments/DEFAULT/modules/singularitee/groups/public",
        }
    )
    void should_not_permit_other_paths(String path) throws Exception {
        int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(401);
    }

    @Test
    void should_build_one_pattern_per_opted_in_module() {
        assertThat(adapter.publicModuleEndpointPatterns()).containsExactly(
            "/organizations/*/environments/*/modules/singularitee/public/**",
            "/organizations/*/environments/*/modules/aihub/public/**"
        );
    }

    @Configuration
    @EnableWebMvc
    @RestController
    static class DummyController {

        @GetMapping(path = { "/openapi-v2.yaml" })
        public void handle() {}
    }
}
