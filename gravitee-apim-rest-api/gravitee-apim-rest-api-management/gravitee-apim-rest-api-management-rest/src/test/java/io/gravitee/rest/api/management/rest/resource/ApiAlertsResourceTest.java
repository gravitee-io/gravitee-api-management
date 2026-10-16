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

import static io.gravitee.rest.api.model.alert.AlertReferenceType.API;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.AlertEventQuery;
import io.gravitee.rest.api.model.alert.AlertEventEntity;
import io.gravitee.rest.api.service.exceptions.AlertNotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
public class ApiAlertsResourceTest extends AbstractResourceTest {

    private static final String API_NAME = "my-api";
    private static final String ALERT_ID = "my-alert";

    @Override
    protected String contextPath() {
        return "apis/" + API_NAME + "/alerts/";
    }

    @BeforeEach
    public void init() {
        reset(alertService);
    }

    @Test
    public void should_get_api_alert_events_scoped_to_the_api() {
        when(alertService.findEvents(eq(ALERT_ID), eq(API), eq(API_NAME), any(AlertEventQuery.class))).thenReturn(
            new Page<>(List.of(new AlertEventEntity()), 1, 1, 1)
        );

        final Response response = envTarget(ALERT_ID + "/events").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        verify(alertService, times(1)).findEvents(eq(ALERT_ID), eq(API), eq(API_NAME), any(AlertEventQuery.class));
    }

    @Test
    public void should_return_404_when_alert_does_not_belong_to_the_api() {
        when(alertService.findEvents(eq(ALERT_ID), eq(API), eq(API_NAME), any(AlertEventQuery.class))).thenThrow(
            new AlertNotFoundException(ALERT_ID)
        );

        final Response response = envTarget(ALERT_ID + "/events").request().get();

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
}
