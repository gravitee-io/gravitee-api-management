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
package io.gravitee.rest.api.management.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class ApplicationResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "applications/123";
    }

    @Test
    public void should_not_update_application_having_unsupported_picture() {
        var entity = UpdateApplicationEntity.builder()
            .name("My beautiful application")
            .description("my description")
            .settings(new ApplicationSettings())
            .picture("data:image/svg+xml;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")
            .build();

        final Response response = envTarget().request().put(Entity.json(entity));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            soft
                .assertThat(response.readEntity(String.class))
                .contains("Invalid image format : Image mime-type image/svg+xml is not allowed");
        });
    }

    @Test
    public void should_not_update_application_having_unsupported_background() {
        var entity = UpdateApplicationEntity.builder()
            .name("My beautiful application")
            .description("my description")
            .settings(new ApplicationSettings())
            .background("data:image/svg+xml;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")
            .build();

        final Response response = envTarget().request().put(Entity.json(entity));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            soft
                .assertThat(response.readEntity(String.class))
                .contains("Invalid image format : Image mime-type image/svg+xml is not allowed");
        });
    }

    @Test
    public void should_update_application() {
        var entity = UpdateApplicationEntity.builder()
            .name("My beautiful application")
            .description("my description")
            .settings(new ApplicationSettings())
            .picture("data:image/png;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")
            .background("data:image/png;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")
            .build();

        when(applicationService.update(any(), eq("123"), eq(entity))).thenReturn(ApplicationEntity.builder().id("123").build());

        final Response response = envTarget().request().put(Entity.json(entity));
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
    }
}
