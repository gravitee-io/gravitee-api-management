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
package io.gravitee.gamma.rest.resources.gravitee_plugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resources.gravitee_plugin.PolicyPluginResourceTest.PolicyPluginTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

/** Without the authenticating filter of the other resource tests: every request is anonymous. */
@ContextConfiguration(classes = { ResourceContextConfiguration.class, PolicyPluginTestConfiguration.class })
class PolicyPluginResourceAuthenticationTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/plugins/policies";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(AnonymousRequestFilter.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "guardrail-rule", "guardrail-rule/schema", "guardrail-rule/documentation" })
    void should_return_401_when_not_authenticated(String path) {
        assertThat(rootTarget(path).request().get().getStatus()).as("GET /%s", path).isEqualTo(HttpStatusCode.UNAUTHORIZED_401);
    }

    /** Server threads may keep the authentication another test class left behind. */
    @Priority(1)
    public static class AnonymousRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) {
            SecurityContextHolder.clearContext();
        }
    }
}
