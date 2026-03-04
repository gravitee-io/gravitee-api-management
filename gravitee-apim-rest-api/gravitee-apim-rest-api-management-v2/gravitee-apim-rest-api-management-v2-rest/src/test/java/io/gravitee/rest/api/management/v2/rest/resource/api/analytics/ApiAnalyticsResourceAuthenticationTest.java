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
package io.gravitee.rest.api.management.v2.rest.resource.api.analytics;

import static io.gravitee.common.http.HttpStatusCode.UNAUTHORIZED_401;

import assertions.MAPIAssertions;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiAnalyticsResourceAuthenticationTest extends ApiResourceTest {

    private WebTarget analyticsTarget;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/analytics";
    }

    @BeforeEach
    void prepareTarget() {
        analyticsTarget = rootTarget();
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        resourceConfig.register(
            new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(response).to(HttpServletResponse.class);
                }
            }
        );
    }

    @Test
    void should_return_401_if_user_is_not_authenticated() {
        final Response response = analyticsTarget.queryParam("type", "COUNT").queryParam("from", 0L).queryParam("to", 1L).request().get();

        MAPIAssertions
            .assertThat(response)
            .hasStatus(UNAUTHORIZED_401)
            .asError()
            .hasHttpStatus(UNAUTHORIZED_401)
            .hasMessage("You must be authenticated to access this resource");
    }
}
