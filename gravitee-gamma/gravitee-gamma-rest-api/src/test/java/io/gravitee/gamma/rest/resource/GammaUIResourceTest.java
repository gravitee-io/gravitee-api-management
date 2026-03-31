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
package io.gravitee.gamma.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.resources.GammaUIResource;
import io.gravitee.gamma.rest.spring.ConfigurableInstallationAccessQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GammaUIResourceTest extends AbstractResourceTest {

    @Inject
    private ConfigurableInstallationAccessQueryService installationAccessQueryService;

    @Override
    protected String contextPath() {
        return "/ui/bootstrap";
    }

    @BeforeEach
    public void init() {
        GraviteeContext.fromExecutionContext(new ExecutionContext(ORGANIZATION));
    }

    @AfterEach
    public void resetInstallationService() {
        installationAccessQueryService.reset();
    }

    @Nested
    class Bootstrap {

        @Test
        void should_return_200() {
            final Response response = rootTarget().request().get();
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_return_bootstrap_with_organization_from_context() {
            final Response response = rootTarget().request().get();

            var body = response.readEntity(GammaUIResource.GammaBootstrap.class);
            assertThat(body.organizationId()).isEqualTo(ORGANIZATION);
        }

        @Test
        void should_use_enforced_organization_id_when_provided() {
            final Response response = rootTarget().queryParam("organizationId", "custom-org").request().get();

            var body = response.readEntity(GammaUIResource.GammaBootstrap.class);
            assertThat(body.organizationId()).isEqualTo("custom-org");
        }

        @Test
        void should_build_gamma_api_url_from_request_when_service_returns_null() {
            final Response response = rootTarget().request().get();

            var body = response.readEntity(GammaUIResource.GammaBootstrap.class);
            assertThat(body.gammaBaseURL()).endsWith("/gamma");
        }

        @Test
        void should_build_management_api_url_from_request_when_service_returns_null() {
            final Response response = rootTarget().request().get();

            var body = response.readEntity(GammaUIResource.GammaBootstrap.class);
            assertThat(body.managementBaseURL()).endsWith("/management");
        }

        @Test
        void should_use_gamma_api_url_from_service() {
            installationAccessQueryService.setGammaAPIUrl("http://gamma.example.com/gamma");

            final Response response = rootTarget().request().get();

            var body = response.readEntity(GammaUIResource.GammaBootstrap.class);
            assertThat(body.gammaBaseURL()).isEqualTo("http://gamma.example.com/gamma");
        }

        @Test
        void should_use_management_api_url_from_service() {
            installationAccessQueryService.setConsoleAPIUrl("http://management.example.com/management");

            final Response response = rootTarget().request().get();

            var body = response.readEntity(GammaUIResource.GammaBootstrap.class);
            assertThat(body.managementBaseURL()).isEqualTo("http://management.example.com/management");
        }
    }
}
