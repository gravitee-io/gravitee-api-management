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
package io.gravitee.rest.api.management.v2.rest.resource.boostrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.license.NodeLicenseService;
import io.gravitee.rest.api.management.v2.rest.model.ConsoleCustomization;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ManagementUIResourceTest extends AbstractResourceTest {

    @Inject
    NodeLicenseService nodeLicenseService;

    @BeforeEach
    public void init() {
        GraviteeContext.fromExecutionContext(new ExecutionContext(ORGANIZATION));
    }

    @Override
    protected String contextPath() {
        return "/ui/customization";
    }

    @Test
    public void should_get_console_customization() {
        when(nodeLicenseService.isFeatureEnabled("oem-customization")).thenReturn(true);
        final Response response = rootTarget().request().get();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);

        var body = response.readEntity(ConsoleCustomization.class);
        assertThat(body.getTitle()).isEqualTo("Celigo");
    }

    @Test
    public void should_return_no_content_if_license_is_not_oem() {
        when(nodeLicenseService.isFeatureEnabled("oem-customization")).thenReturn(false);
        final Response response = rootTarget().request().get();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
    }
}
