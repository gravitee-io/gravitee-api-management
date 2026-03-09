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
package io.gravitee.rest.api.management.v2.rest.resource.installation;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.ConsoleApplication;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
public class InstallationResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "/installation/applications";
    }

    @Test
    public void shouldReturnListOfConsoleApplications() {
        final Response response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);

        List<ConsoleApplication> applications = response.readEntity(new GenericType<>() {});
        assertThat(applications).isNotNull().hasSize(3);

        ConsoleApplication appAlpha = applications
            .stream()
            .filter(a -> "app-alpha".equals(a.key()))
            .findFirst()
            .orElseThrow();
        assertThat(appAlpha.key()).isEqualTo("app-alpha");
        assertThat(appAlpha.title()).isEqualTo("App Alpha");
        assertThat(appAlpha.icon()).isEqualTo("Box");

        ConsoleApplication appBeta = applications
            .stream()
            .filter(a -> "app-beta".equals(a.key()))
            .findFirst()
            .orElseThrow();
        assertThat(appBeta.key()).isEqualTo("app-beta");
        assertThat(appBeta.title()).isEqualTo("App Beta");
        assertThat(appBeta.icon()).isEqualTo("Settings");

        ConsoleApplication developerPortal = applications
            .stream()
            .filter(a -> "developer-portal".equals(a.key()))
            .findFirst()
            .orElseThrow();
        assertThat(developerPortal.key()).isEqualTo("developer-portal");
        assertThat(developerPortal.title()).isEqualTo("Developer Portal");
        assertThat(developerPortal.icon()).isEqualTo("Globe");
    }
}
