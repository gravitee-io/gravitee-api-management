/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.alert.api.trigger.Trigger.Severity.INFO;
import static io.gravitee.rest.api.model.alert.AlertReferenceType.ENVIRONMENT;
import static io.gravitee.rest.api.service.common.GraviteeContext.getCurrentEnvironment;
import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.resource.param.AlertAnalyticsParam;
import io.gravitee.rest.api.model.AlertAnalyticsQuery;
import io.gravitee.rest.api.model.alert.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class PlatformAlertsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "platform/alerts/";
    }

    @Before
    public void setUp() {
        reset(alertService, alertAnalyticsService);
    }

    private class TestAlertTrigger extends AlertTriggerEntity {

        protected TestAlertTrigger(String id) {
            super(id, "name", "source", INFO, true);
        }
    }

    @Test
    public void should_list_with_event_count() {
        List<AlertTriggerEntity> alerts = List.of(
            new TestAlertTrigger("alert-1"),
            new TestAlertTrigger("alert-2"),
            new TestAlertTrigger("alert-3")
        );

        when(alertService.findByReferenceWithEventCounts(ENVIRONMENT, getCurrentEnvironment())).thenReturn(alerts);

        final Response response = envTarget().queryParam("event_counts", true).request().get();

        List<Map> resultEntities = (List<Map>) (response.readEntity(List.class));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertEquals(3, resultEntities.size());
        assertEquals("alert-1", resultEntities.get(0).get("id"));
        assertEquals("alert-2", resultEntities.get(1).get("id"));
        assertEquals("alert-3", resultEntities.get(2).get("id"));
    }

    @Test
    public void should_list_without_event_count() {
        List<AlertTriggerEntity> alerts = List.of(
            new TestAlertTrigger("alert-1"),
            new TestAlertTrigger("alert-2"),
            new TestAlertTrigger("alert-3")
        );

        when(alertService.findByReference(ENVIRONMENT, getCurrentEnvironment())).thenReturn(alerts);

        final Response response = envTarget().queryParam("event_counts", false).request().get();

        List<Map> resultEntities = (List<Map>) (response.readEntity(List.class));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertEquals(3, resultEntities.size());
        assertEquals("alert-1", resultEntities.get(0).get("id"));
        assertEquals("alert-2", resultEntities.get(1).get("id"));
        assertEquals("alert-3", resultEntities.get(2).get("id"));
    }

    @Test
    public void should_get_platform_alert_analytics() {
        AlertAnalyticsEntity alerts = new AlertAnalyticsEntity(
            Map.of(),
            List.of(
                new AlertAnalyticsEntity.AlertTriggerAnalytics(),
                new AlertAnalyticsEntity.AlertTriggerAnalytics(),
                new AlertAnalyticsEntity.AlertTriggerAnalytics()
            )
        );

        AlertAnalyticsParam param = new AlertAnalyticsParam();
        param.setFrom(0L);
        param.setTo(0L);

        when(alertAnalyticsService.findByReference(eq(ENVIRONMENT), eq(getCurrentEnvironment()), any(AlertAnalyticsQuery.class)))
            .thenReturn(alerts);

        final Response response = envTarget("analytics").queryParam("from", 12).queryParam("to", 157).request().get();

        verify(alertAnalyticsService, times(1))
            .findByReference(eq(ENVIRONMENT), eq(getCurrentEnvironment()), argThat(query -> query.getFrom() == 12 && query.getTo() == 157));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void should_get_platform_alert_status() {
        AlertStatusEntity status = new AlertStatusEntity();
        status.setEnabled(true);
        status.setPlugins(6);

        when(alertService.getStatus(GraviteeContext.getExecutionContext())).thenReturn(status);

        final Response response = envTarget("status").request().get();
        AlertStatusEntity resultEntity = response.readEntity(AlertStatusEntity.class);

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertEquals(true, resultEntity.isEnabled());
        assertEquals(6, resultEntity.getPlugins());
    }

    @Test
    public void should_create_alert() {
        NewAlertTriggerEntity newAlertTriggerEntity = new NewAlertTriggerEntity();
        newAlertTriggerEntity.setId("my-alert");
        newAlertTriggerEntity.setName("my-alert-name");
        newAlertTriggerEntity.setType("my-alert-type");

        final Response response = envTarget().request().post(Entity.json(newAlertTriggerEntity));

        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        verify(alertService, times(1))
            .create(
                eq(getExecutionContext()),
                argThat(alert -> alert.getId().equals("my-alert") && alert.getReferenceType() == ENVIRONMENT)
            );
    }

    @Test
    public void should_update_alert() {
        UpdateAlertTriggerEntity updateAlertTriggerEntity = new UpdateAlertTriggerEntity();
        updateAlertTriggerEntity.setId("my-alert");
        updateAlertTriggerEntity.setName("my-alert-name");
        updateAlertTriggerEntity.setSeverity(INFO);

        final Response response = envTarget().path("my-alert").request().put(Entity.json(updateAlertTriggerEntity));

        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        verify(alertService, times(1))
            .update(
                eq(getExecutionContext()),
                argThat(alert -> alert.getId().equals("my-alert") && alert.getReferenceType() == ENVIRONMENT)
            );
    }

    @Test
    public void should_delete_alert() {
        final Response response = envTarget().path("my-alert").request().delete();

        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        verify(alertService, times(1)).delete("my-alert", getCurrentEnvironment());
    }
}
