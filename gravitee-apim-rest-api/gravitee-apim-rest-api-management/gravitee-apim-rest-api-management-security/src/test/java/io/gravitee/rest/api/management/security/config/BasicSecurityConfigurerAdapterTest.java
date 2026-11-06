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
package io.gravitee.rest.api.management.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.gravitee.rest.api.security.config.AbstractSecurityConfigurerAdapterTest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @Override
    protected String getPath() {
        return "/openapi.json";
    }

    @Test
    void should_let_the_cockpit_login_form_through() throws Exception {
        postLoginFormFromCockpit("/auth/cockpit").andExpect(status().isOk());
    }

    @Test
    void should_not_let_the_cockpit_login_form_through_on_another_path() throws Exception {
        postLoginFormFromCockpit("/auth/cockpitx").andExpect(status().isForbidden());
    }

    private ResultActions postLoginFormFromCockpit(String path) throws Exception {
        return mockMvc.perform(
            post(path)
                .with(request -> {
                    request.setPathInfo(path);
                    return request;
                })
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "login-token")
                .header(HttpHeaders.ORIGIN, "https://cockpit.example.com")
        );
    }

    @Configuration
    @EnableWebMvc
    @RestController
    static class DummyController {

        @GetMapping(path = { "/openapi.json" })
        public void handle() {}

        @PostMapping(path = { "/auth/cockpit", "/auth/cockpitx" })
        public void login() {}
    }
}
