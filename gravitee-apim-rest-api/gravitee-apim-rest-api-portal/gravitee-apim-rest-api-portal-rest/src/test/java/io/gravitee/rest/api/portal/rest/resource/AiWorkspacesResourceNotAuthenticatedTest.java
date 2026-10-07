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
package io.gravitee.rest.api.portal.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

class AiWorkspacesResourceNotAuthenticatedTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "ai-workspaces/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAuthenticatedAuthenticationFilter.class);
    }

    @Test
    void anonymous_caller_is_unauthorized() {
        assertThat(target().request().get().getStatus()).isEqualTo(401);
        assertThat(target().path("ws-1").request().get().getStatus()).isEqualTo(401);
        assertThat(target().path("ws-1").path("consumption").request().get().getStatus()).isEqualTo(401);
    }
}
