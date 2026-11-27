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
package io.gravitee.rest.api.portal.security.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.security.config.AbstractSecurityConfigurerAdapterTest;
import io.gravitee.rest.api.service.EnvironmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
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
class BasicSecurityConfigurerAdapterTest extends AbstractSecurityConfigurerAdapterTest {

    private static final String API_PATH = "/environments/DEFAULT/apis/my-api";
    private static final String PORTAL_PATH = "/environments/DEFAULT/portal/6c2b1e9e-0000-4000-8000-000000000000";

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void stub_default_environment() {
        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId("DEFAULT");
        environment.setOrganizationId("DEFAULT");
        when(environmentService.findById(any())).thenReturn(environment);
        when(environmentService.findByOrgAndIdOrHrid(any(), any())).thenReturn(environment);
    }

    @Override
    protected String getPath() {
        return "/openapi";
    }

    @ParameterizedTest
    @ValueSource(strings = { "/subscribers", "/subscribers/", "/subscribers/?page=1&size=500" })
    void should_apply_the_same_rules_with_and_without_a_trailing_slash(String subscribersPath) throws Exception {
        mockMvc.perform(get(API_PATH + subscribersPath)).andExpect(status().isUnauthorized());
    }

    @Test
    void should_leave_other_paths_unchanged_with_a_trailing_slash() throws Exception {
        mockMvc.perform(get(API_PATH + "/metrics/")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "/plans", "/subscription-form" })
    void should_let_anonymous_users_read_an_api_page_on_a_portal(String subPath) throws Exception {
        mockMvc.perform(get(PORTAL_PATH + "/apis/my-api" + subPath)).andExpect(status().isNotFound());
    }

    @Test
    void should_require_authentication_to_read_an_api_product_on_a_portal() throws Exception {
        mockMvc.perform(get(PORTAL_PATH + "/api-products/my-api-product")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = { "/subscribers", "/subscribers/" })
    void should_require_authentication_to_read_the_subscribers_of_an_api_on_a_portal(String subscribersPath) throws Exception {
        mockMvc.perform(get(PORTAL_PATH + "/apis/my-api" + subscribersPath)).andExpect(status().isUnauthorized());
    }

    @Test
    void should_require_authentication_to_subscribe_on_a_portal() throws Exception {
        mockMvc
            .perform(post(PORTAL_PATH + "/subscriptions").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Configuration
    @EnableWebMvc
    @RestController
    static class DummyController {

        @GetMapping(path = { "/openapi" })
        public void handle() {}
    }
}
