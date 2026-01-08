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
package io.gravitee.apim.rest.api.automation.security.config;

import io.gravitee.rest.api.security.config.AbstractSecurityConfigurerAdapterTest;
import org.springframework.context.annotation.Configuration;
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

    @Override
    protected String getPath() {
        return "/open-api.yaml";
    }

    @Configuration
    @EnableWebMvc
    @RestController
    static class DummyController {

        @GetMapping(path = { "/open-api.yaml" })
        public void handle() {}
    }
}
